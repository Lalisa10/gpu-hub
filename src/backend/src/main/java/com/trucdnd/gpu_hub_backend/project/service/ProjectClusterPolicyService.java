package com.trucdnd.gpu_hub_backend.project.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.policy.entity.Policy;
import com.trucdnd.gpu_hub_backend.policy.repository.PolicyRepository;
import com.trucdnd.gpu_hub_backend.project.dto.CreateProjectClusterPolicyRequest;
import com.trucdnd.gpu_hub_backend.project.dto.PatchProjectClusterPolicyRequest;
import com.trucdnd.gpu_hub_backend.project.dto.ProjectClusterPolicyDto;
import com.trucdnd.gpu_hub_backend.project.dto.UpdateProjectClusterPolicyRequest;
import com.trucdnd.gpu_hub_backend.project.entity.Project;
import com.trucdnd.gpu_hub_backend.project.entity.ProjectClusterPolicy;
import com.trucdnd.gpu_hub_backend.project.repository.ProjectClusterPolicyRepository;
import com.trucdnd.gpu_hub_backend.project.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectClusterPolicyService {
    private final ProjectClusterPolicyRepository projectClusterPolicyRepository;
    private final ProjectRepository projectRepository;
    private final ClusterRepository clusterRepository;
    private final PolicyRepository policyRepository;

    public List<ProjectClusterPolicyDto> findAll() {
        return projectClusterPolicyRepository.findAll().stream().map(this::toDto).toList();
    }

    public ProjectClusterPolicyDto findById(UUID id) {
        return toDto(getProjectClusterPolicy(id));
    }

    public ProjectClusterPolicyDto create(CreateProjectClusterPolicyRequest request) {
        ProjectClusterPolicy entity = new ProjectClusterPolicy();
        apply(entity, request.projectId(), request.clusterId(), request.policyId());
        return toDto(projectClusterPolicyRepository.save(entity));
    }

    public ProjectClusterPolicyDto update(UUID id, UpdateProjectClusterPolicyRequest request) {
        ProjectClusterPolicy entity = getProjectClusterPolicy(id);
        apply(entity, request.projectId(), request.clusterId(), request.policyId());
        return toDto(projectClusterPolicyRepository.save(entity));
    }

    public ProjectClusterPolicyDto patch(UUID id, PatchProjectClusterPolicyRequest request) {
        ProjectClusterPolicy entity = getProjectClusterPolicy(id);

        if (request.projectId().isPresent()) {
            Project project = projectRepository.findById(request.projectId().orElse(null))
                    .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + request.projectId().orElse(null)));
            entity.setProject(project);
        }

        UUID clusterId = request.clusterId().orElse(entity.getCluster().getId());
        UUID policyId = request.policyId().orElse(entity.getPolicy().getId());
        if (request.clusterId().isPresent() || request.policyId().isPresent()) {
            Cluster cluster = clusterRepository.findById(clusterId)
                    .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + clusterId));
            Policy policy = policyRepository.findById(policyId)
                    .orElseThrow(() -> new EntityNotFoundException("Policy not found with id: " + policyId));

            if (!policy.getCluster().getId().equals(clusterId)) {
                throw new IllegalArgumentException("Policy does not belong to the provided cluster");
            }

            entity.setCluster(cluster);
            entity.setPolicy(policy);
        }

        return toDto(projectClusterPolicyRepository.save(entity));
    }

    public void delete(UUID id) {
        projectClusterPolicyRepository.delete(getProjectClusterPolicy(id));
    }

    private void apply(ProjectClusterPolicy target, UUID projectId, UUID clusterId, UUID policyId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + clusterId));
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found with id: " + policyId));

        if (!policy.getCluster().getId().equals(clusterId)) {
            throw new IllegalArgumentException("Policy does not belong to the provided cluster");
        }

        target.setProject(project);
        target.setCluster(cluster);
        target.setPolicy(policy);
    }

    private ProjectClusterPolicy getProjectClusterPolicy(UUID id) {
        return projectClusterPolicyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ProjectClusterPolicy not found with id: " + id));
    }

    private ProjectClusterPolicyDto toDto(ProjectClusterPolicy entity) {
        return new ProjectClusterPolicyDto(
                entity.getId(),
                entity.getProject().getId(),
                entity.getCluster().getId(),
                entity.getPolicy().getId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
