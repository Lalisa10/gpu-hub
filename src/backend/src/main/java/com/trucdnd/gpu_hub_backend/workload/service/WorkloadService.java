package com.trucdnd.gpu_hub_backend.workload.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.project.entity.Project;
import com.trucdnd.gpu_hub_backend.project.repository.ProjectRepository;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import com.trucdnd.gpu_hub_backend.user.repository.UserRepository;
import com.trucdnd.gpu_hub_backend.workload.dto.CreateWorkloadRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.PatchWorkloadRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.UpdateWorkloadRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.WorkloadDto;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;
import com.trucdnd.gpu_hub_backend.workload.entity.WorkloadType;
import com.trucdnd.gpu_hub_backend.workload.repository.WorkloadRepository;
import com.trucdnd.gpu_hub_backend.workload.repository.WorkloadTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkloadService {
    private final WorkloadRepository workloadRepository;
    private final ProjectRepository projectRepository;
    private final ClusterRepository clusterRepository;
    private final UserRepository userRepository;
    private final WorkloadTypeRepository workloadTypeRepository;

    public List<WorkloadDto> findAll() {
        return workloadRepository.findAll().stream().map(this::toDto).toList();
    }

    public WorkloadDto findById(UUID id) {
        return toDto(getWorkload(id));
    }

    public WorkloadDto create(CreateWorkloadRequest request) {
        Workload entity = new Workload();
        apply(entity, request.projectId(), request.clusterId(), request.submittedById(), request.workloadTypeId(),
                request.name(), request.priority(), request.requestedGpu(), request.requestedCpu(), request.requestedMemory(),
                request.status(), request.k8sNamespace(), request.k8sResourceName(), request.k8sResourceKind(),
                request.queuedAt(), request.startedAt(), request.finishedAt(), request.extra());
        return toDto(workloadRepository.save(entity));
    }

    public WorkloadDto update(UUID id, UpdateWorkloadRequest request) {
        Workload entity = getWorkload(id);
        apply(entity, request.projectId(), request.clusterId(), request.submittedById(), request.workloadTypeId(),
                request.name(), request.priority(), request.requestedGpu(), request.requestedCpu(), request.requestedMemory(),
                request.status(), request.k8sNamespace(), request.k8sResourceName(), request.k8sResourceKind(),
                request.queuedAt(), request.startedAt(), request.finishedAt(), request.extra());
        return toDto(workloadRepository.save(entity));
    }

    public WorkloadDto patch(UUID id, PatchWorkloadRequest request) {
        Workload entity = getWorkload(id);
        Project resolvedProject = entity.getProject();
        Cluster resolvedCluster = entity.getCluster();

        if (request.projectId().isPresent()) {
            Project project = projectRepository.findById(request.projectId().orElse(null))
                    .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + request.projectId().orElse(null)));
            entity.setProject(project);
            resolvedProject = project;
        }
        if (request.clusterId().isPresent()) {
            Cluster cluster = clusterRepository.findById(request.clusterId().orElse(null))
                    .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + request.clusterId().orElse(null)));
            entity.setCluster(cluster);
            resolvedCluster = cluster;
        }
        if (request.submittedById().isPresent()) {
            User submittedBy = userRepository.findById(request.submittedById().orElse(null))
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.submittedById().orElse(null)));
            entity.setSubmittedBy(submittedBy);
        }
        if (request.workloadTypeId().isPresent()) {
            WorkloadType workloadType = workloadTypeRepository.findById(request.workloadTypeId().orElse(null))
                    .orElseThrow(() -> new EntityNotFoundException("WorkloadType not found with id: " + request.workloadTypeId().orElse(null)));
            entity.setWorkloadType(workloadType);
        }
        if (request.name().isPresent()) {
            entity.setName(request.name().orElse(null));
        }
        if (request.priority().isPresent()) {
            entity.setPriority(request.priority().orElse(null));
        }
        if (request.requestedGpu().isPresent()) {
            entity.setRequestedGpu(request.requestedGpu().orElse(null));
        }
        if (request.requestedCpu().isPresent()) {
            entity.setRequestedCpu(request.requestedCpu().orElse(null));
        }
        if (request.requestedMemory().isPresent()) {
            entity.setRequestedMemory(request.requestedMemory().orElse(null));
        }
        if (request.status().isPresent()) {
            entity.setStatus(request.status().orElse(null));
        }
        if (request.k8sNamespace().isPresent()) {
            entity.setK8sNamespace(request.k8sNamespace().orElse(null));
        }
        if (request.k8sResourceName().isPresent()) {
            entity.setK8sResourceName(request.k8sResourceName().orElse(null));
        }
        if (request.k8sResourceKind().isPresent()) {
            entity.setK8sResourceKind(request.k8sResourceKind().orElse(null));
        }
        if (request.queuedAt().isPresent()) {
            entity.setQueuedAt(request.queuedAt().orElse(null));
        }
        if (request.startedAt().isPresent()) {
            entity.setStartedAt(request.startedAt().orElse(null));
        }
        if (request.finishedAt().isPresent()) {
            entity.setFinishedAt(request.finishedAt().orElse(null));
        }
        if (request.extra().isPresent()) {
            entity.setExtra(request.extra().orElse(null));
        }

        validateProjectCluster(resolvedProject, resolvedCluster.getId());
        return toDto(workloadRepository.save(entity));
    }

    public void delete(UUID id) {
        workloadRepository.delete(getWorkload(id));
    }

    private void apply(
            Workload entity,
            UUID projectId,
            UUID clusterId,
            UUID submittedById,
            UUID workloadTypeId,
            String name,
            Integer priority,
            BigDecimal requestedGpu,
            BigDecimal requestedCpu,
            Long requestedMemory,
            String status,
            String k8sNamespace,
            String k8sResourceName,
            String k8sResourceKind,
            OffsetDateTime queuedAt,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            String extra) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + clusterId));
        User submittedBy = userRepository.findById(submittedById)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + submittedById));
        WorkloadType workloadType = workloadTypeRepository.findById(workloadTypeId)
                .orElseThrow(() -> new EntityNotFoundException("WorkloadType not found with id: " + workloadTypeId));

        validateProjectCluster(project, clusterId);

        entity.setProject(project);
        entity.setCluster(cluster);
        entity.setSubmittedBy(submittedBy);
        entity.setWorkloadType(workloadType);
        entity.setName(name);
        entity.setPriority(priority);
        entity.setRequestedGpu(requestedGpu);
        entity.setRequestedCpu(requestedCpu);
        entity.setRequestedMemory(requestedMemory);
        entity.setStatus(status);
        entity.setK8sNamespace(k8sNamespace);
        entity.setK8sResourceName(k8sResourceName);
        entity.setK8sResourceKind(k8sResourceKind);
        entity.setQueuedAt(queuedAt);
        entity.setStartedAt(startedAt);
        entity.setFinishedAt(finishedAt);
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
                entity.getWorkloadType().getId(),
                entity.getName(),
                entity.getPriority(),
                entity.getRequestedGpu(),
                entity.getRequestedCpu(),
                entity.getRequestedMemory(),
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
}
