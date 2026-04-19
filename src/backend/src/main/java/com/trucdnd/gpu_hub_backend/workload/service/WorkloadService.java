package com.trucdnd.gpu_hub_backend.workload.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.kubernetes.service.BuiltinResourceService;
import com.trucdnd.gpu_hub_backend.kubernetes.service.NotebookService;
import com.trucdnd.gpu_hub_backend.project.entity.Project;
import com.trucdnd.gpu_hub_backend.project.repository.ProjectRepository;
import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;
import com.trucdnd.gpu_hub_backend.team.repository.TeamClusterRepository;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import com.trucdnd.gpu_hub_backend.user.repository.UserRepository;
import com.trucdnd.gpu_hub_backend.workload.dto.CreateWorkloadRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.PodInfoDto;
import com.trucdnd.gpu_hub_backend.workload.dto.WorkloadDto;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;
import com.trucdnd.gpu_hub_backend.workload.repository.WorkloadRepository;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.trucdnd.gpu_hub_backend.common.constants.Workload.PriorityClass;
import com.trucdnd.gpu_hub_backend.common.constants.Workload.Status;
import com.trucdnd.gpu_hub_backend.common.constants.Workload.Type;
import com.trucdnd.gpu_hub_backend.common.utils.RandomK8sResourceNameGenerator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkloadService {
    private final WorkloadRepository workloadRepository;
    private final ProjectRepository projectRepository;
    private final ClusterRepository clusterRepository;
    private final UserRepository userRepository;
    private final TeamClusterRepository teamClusterRepository;
    private final NotebookService notebookService;
    private final NotebookSpecBuilder notebookSpecBuilder;
    private final DeploymentSpecBuilder deploymentSpecBuilder;
    private final BuiltinResourceService builtinResourceService;
    private final RandomK8sResourceNameGenerator nameGenerator;

    public List<WorkloadDto> findAll() {
        return workloadRepository.findAll().stream().map(this::toDto).toList();
    }

    public WorkloadDto findById(UUID id) {
        return toDto(getWorkload(id));
    }

    public WorkloadDto create(CreateWorkloadRequest request) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.projectId()));
        Cluster cluster = clusterRepository.findById(request.clusterId())
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found: " + request.clusterId()));
        User submittedBy = userRepository.findById(request.submittedById())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.submittedById()));

        if (!project.getCluster().getId().equals(cluster.getId())) {
            throw new IllegalArgumentException("cluster_id of workload must match cluster_id of project");
        }

        TeamCluster teamCluster = teamClusterRepository
                .findByTeam_IdAndCluster_Id(project.getTeam().getId(), cluster.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "TeamCluster not found for team=" + project.getTeam().getId()
                                + " cluster=" + cluster.getId()));

        Workload entity = Workload.builder()
                .project(project)
                .cluster(cluster)
                .submittedBy(submittedBy)
                .workloadType(request.workloadType())
                .priorityClass(PriorityClass.TRAIN)
                .name(request.name())
                .image(request.image())
                .requestedGpu(request.requestedGpu())
                .requestedCpu(request.requestedCpu())
                .requestedMemory(request.requestedMemory())
                .status(Status.PENDING)
                .extra(request.extra())
                .build();

        Workload saved = workloadRepository.save(entity);

        String k8sName = buildK8sName(saved);
        submit(saved, k8sName, teamCluster.getNamespace());

        return toDto(saved);
    }

    public void delete(UUID id) {
        workloadRepository.delete(getWorkload(id));
    }

    public List<PodInfoDto> listPods(UUID workloadId) {
        Workload workload = getWorkload(workloadId);
        String namespace = resolveNamespace(workload);
        Map<String, String> selector = Map.of(NotebookSpecBuilder.WORKLOAD_ID_LABEL, workload.getId().toString());
        List<Pod> pods = builtinResourceService.listPodsByLabel(workload.getCluster(), namespace, selector);
        return pods.stream().map(this::toPodDto).toList();
    }

    public String getPodLogs(UUID workloadId, String podName) {
        Workload workload = getWorkload(workloadId);
        String namespace = resolveNamespace(workload);
        Map<String, String> selector = Map.of(NotebookSpecBuilder.WORKLOAD_ID_LABEL, workload.getId().toString());
        boolean owned = builtinResourceService.listPodsByLabel(workload.getCluster(), namespace, selector).stream()
                .anyMatch(p -> podName.equals(p.getMetadata().getName()));
        if (!owned) {
            throw new EntityNotFoundException("Pod '" + podName + "' does not belong to workload " + workloadId);
        }
        return builtinResourceService.getPodLogs(workload.getCluster(), namespace, podName);
    }

    private String resolveNamespace(Workload workload) {
        TeamCluster teamCluster = teamClusterRepository
                .findByTeam_IdAndCluster_Id(workload.getProject().getTeam().getId(), workload.getCluster().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "TeamCluster not found for team=" + workload.getProject().getTeam().getId()
                                + " cluster=" + workload.getCluster().getId()));
        return teamCluster.getNamespace();
    }

    private PodInfoDto toPodDto(Pod pod) {
        String phase = pod.getStatus() != null ? pod.getStatus().getPhase() : null;
        String ip = pod.getStatus() != null ? pod.getStatus().getPodIP() : null;
        String nodeName = pod.getSpec() != null ? pod.getSpec().getNodeName() : null;
        OffsetDateTime startTime = null;
        if (pod.getStatus() != null && pod.getStatus().getStartTime() != null) {
            try {
                startTime = OffsetDateTime.parse(pod.getStatus().getStartTime());
            } catch (Exception ignored) {
                // leave startTime null if parsing fails
            }
        }

        String detailedStatus = phase;
        boolean ready = false;
        int restartCount = 0;
        if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null
                && !pod.getStatus().getContainerStatuses().isEmpty()) {
            ContainerStatus cs = pod.getStatus().getContainerStatuses().get(0);
            ready = Boolean.TRUE.equals(cs.getReady());
            restartCount = cs.getRestartCount() != null ? cs.getRestartCount() : 0;
            if (cs.getState() != null) {
                if (cs.getState().getWaiting() != null && cs.getState().getWaiting().getReason() != null) {
                    detailedStatus = cs.getState().getWaiting().getReason();
                } else if (cs.getState().getTerminated() != null && cs.getState().getTerminated().getReason() != null) {
                    detailedStatus = cs.getState().getTerminated().getReason();
                } else if (cs.getState().getRunning() != null) {
                    detailedStatus = "Running";
                }
            }
        }

        return new PodInfoDto(
                pod.getMetadata().getName(),
                ip,
                nodeName,
                phase,
                detailedStatus,
                ready,
                restartCount,
                startTime
        );
    }

    private Workload getWorkload(UUID id) {
        return workloadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workload not found: " + id));
    }

    /** Generates the K8s resource name: {@code <username>-<kind>-<random5>}. */
    private String buildK8sName(Workload workload) {
        String kind = workload.getWorkloadType() == Type.NOTEBOOK ? "notebook" : "deployment";
        return nameGenerator.generateWorkloadName(workload.getSubmittedBy().getUsername(), kind);
    }

    private void submit(Workload workload, String k8sName, String namespace) {
        String queueName = workload.getProject().getName();
        switch (workload.getWorkloadType()) {
            case NOTEBOOK -> {
                GenericKubernetesResource notebook = notebookSpecBuilder.build(workload, k8sName, namespace, queueName);
                notebookService.create(workload.getCluster(), notebook);
            }
            case LLM_INFERENCE -> {
                Deployment deployment = deploymentSpecBuilder.build(workload, k8sName, namespace, queueName);
                builtinResourceService.createDeployment(workload.getCluster(), namespace, deployment);
            }
        }
    }

    private WorkloadDto toDto(Workload entity) {
        return new WorkloadDto(
                entity.getId(),
                entity.getProject().getId(),
                entity.getCluster().getId(),
                entity.getSubmittedBy().getId(),
                entity.getWorkloadType(),
                entity.getPriorityClass(),
                entity.getImage(),
                entity.getName(),
                entity.getRequestedGpu(),
                entity.getRequestedCpu(),
                entity.getRequestedMemory(),
                entity.getStatus(),
                entity.getQueuedAt(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getExtra(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
