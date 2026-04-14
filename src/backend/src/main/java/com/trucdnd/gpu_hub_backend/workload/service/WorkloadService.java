package com.trucdnd.gpu_hub_backend.workload.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.kubernetes.service.NotebookService;
import com.trucdnd.gpu_hub_backend.project.entity.Project;
import com.trucdnd.gpu_hub_backend.project.repository.ProjectRepository;
import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;
import com.trucdnd.gpu_hub_backend.team.repository.TeamClusterRepository;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import com.trucdnd.gpu_hub_backend.user.repository.UserRepository;
import com.trucdnd.gpu_hub_backend.workload.dto.CreateWorkloadRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.WorkloadDto;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;
import com.trucdnd.gpu_hub_backend.workload.repository.WorkloadRepository;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.trucdnd.gpu_hub_backend.common.constants.Workload.Type;
import com.trucdnd.gpu_hub_backend.common.utils.RandomK8sResourceNameGenerator;

import java.math.BigDecimal;
import java.util.List;
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
    private final RandomK8sResourceNameGenerator suffixGenerator;

    public List<WorkloadDto> findAll() {
        return workloadRepository.findAll().stream().map(this::toDto).toList();
    }

    public WorkloadDto findById(UUID id) {
        return toDto(getWorkload(id));
    }

    public WorkloadDto create(CreateWorkloadRequest request) {
        Workload entity = new Workload();
        apply(entity, request.projectId(), request.clusterId(), request.submittedById(), request.workloadType(),
                request.priorityClass(), request.name(), request.requestedGpu(), request.requestedCpu(),
                request.requestedCpuLimit(), request.requestedMemory(), request.requestedMemoryLimit(), request.extra());

        entity.setStatus(com.trucdnd.gpu_hub_backend.common.constants.Workload.Status.QUEUED);
        entity.setK8sResourceKind(convertToResourceKind(entity));
        entity.setK8sResourceName(buildWorkloadName(entity));
        TeamCluster teamCluster = teamClusterRepository
                .findByTeam_IdAndCluster_Id(entity.getProject().getTeam().getId(), entity.getCluster().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "TeamCluster not found for team=" + entity.getProject().getTeam().getId()
                                + " cluster=" + entity.getCluster().getId()));
        entity.setK8sNamespace(teamCluster.getNamespace());
        Workload saved = workloadRepository.save(entity);
        submit(saved);
        return toDto(saved);
    }

    public void delete(UUID id) {
        workloadRepository.delete(getWorkload(id));
    }

    private void apply(
            Workload entity,
            UUID projectId,
            UUID clusterId,
            UUID submittedById,
            com.trucdnd.gpu_hub_backend.common.constants.Workload.Type workloadType,
            com.trucdnd.gpu_hub_backend.common.constants.Workload.PriorityClass  priorityClass,
            String name,
            BigDecimal requestedGpu,
            BigDecimal requestedCpu,
            BigDecimal requestedCpuLimit,
            Long requestedMemory,
            Long requestedMemoryLimit,
            String extra) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + clusterId));
        User submittedBy = userRepository.findById(submittedById)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + submittedById));

        validateProjectCluster(project, clusterId);

        entity.setProject(project);
        entity.setCluster(cluster);
        entity.setSubmittedBy(submittedBy);
        entity.setWorkloadType(workloadType);
        entity.setPriorityClass(priorityClass);
        entity.setName(name);
        entity.setRequestedGpu(requestedGpu);
        entity.setRequestedCpu(requestedCpu);
        entity.setRequestedCpuLimit(requestedCpuLimit);
        entity.setRequestedMemory(requestedMemory);
        entity.setRequestedMemoryLimit(requestedMemoryLimit);
        entity.setExtra(extra);
    }

    private Workload getWorkload(UUID id) {
        return workloadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workload not found with id: " + id));
    }

    private void validateProjectCluster(Project project, UUID clusterId) {
        if (!project.getCluster().getId().equals(clusterId)) {
            throw new IllegalArgumentException("cluster_id of workload must match cluster_id of project");
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
                entity.getName(),
                entity.getRequestedGpu(),
                entity.getRequestedCpu(),
                entity.getRequestedCpuLimit(),
                entity.getRequestedMemory(),
                entity.getRequestedMemoryLimit(),
                entity.getStatus(),
                entity.getK8sNamespace(),
                entity.getK8sResourceName(),
                entity.getK8sResourceKind(),
                entity.getQueuedAt(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getExtra(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String buildWorkloadName(Workload workload) {
        String userName = workload.getSubmittedBy().getUsername();
        String resourceKind = convertToResourceKind(workload);
        String suffix = suffixGenerator.generateString(5);
        return userName + resourceKind + suffix;
    }

    private String convertToResourceKind (Workload workload) {
        if (workload.getWorkloadType() == Type.LLM_INFERENCE) {
            return "Deployment";
        } else {
            return "Notebook";
        }
    }

    private void submit(Workload workload) {
        if (workload.getWorkloadType() == Type.NOTEBOOK) {
            GenericKubernetesResource notebook = notebookSpecBuilder.build(workload, workload.getK8sNamespace());
            notebookService.create(workload.getCluster(), notebook);
        }
    }
}
