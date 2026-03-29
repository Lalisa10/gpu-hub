package com.trucdnd.gpu_hub_backend.project.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.policy.entity.Policy;
import com.trucdnd.gpu_hub_backend.policy.repository.PolicyRepository;
import com.trucdnd.gpu_hub_backend.project.dto.CreateProjectRequest;
import com.trucdnd.gpu_hub_backend.project.dto.PatchProjectRequest;
import com.trucdnd.gpu_hub_backend.project.dto.ProjectDto;
import com.trucdnd.gpu_hub_backend.project.dto.UpdateProjectRequest;
import com.trucdnd.gpu_hub_backend.project.entity.Project;
import com.trucdnd.gpu_hub_backend.project.repository.ProjectRepository;
import com.trucdnd.gpu_hub_backend.team.entity.Team;
import com.trucdnd.gpu_hub_backend.team.repository.TeamClusterRepository;
import com.trucdnd.gpu_hub_backend.team.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final ClusterRepository clusterRepository;
    private final PolicyRepository policyRepository;
    private final TeamClusterRepository teamClusterRepository;

    public List<ProjectDto> findAll() {
        return projectRepository.findAll().stream().map(this::toDto).toList();
    }

    public ProjectDto findById(UUID id) {
        return toDto(getProject(id));
    }

    public ProjectDto create(CreateProjectRequest request) {
        Project project = new Project();
        apply(
                project,
                request.teamId(),
                request.clusterId(),
                request.policyId(),
                request.name(),
                request.description(),
                request.mlflowExperimentId(),
                request.minioPrefix());
        return toDto(projectRepository.save(project));
    }

    public ProjectDto update(UUID id, UpdateProjectRequest request) {
        Project project = getProject(id);
        apply(
                project,
                request.teamId(),
                request.clusterId(),
                request.policyId(),
                request.name(),
                request.description(),
                request.mlflowExperimentId(),
                request.minioPrefix());
        return toDto(projectRepository.save(project));
    }

    public ProjectDto patch(UUID id, PatchProjectRequest request) {
        Project project = getProject(id);

        UUID teamId = request.teamId().orElse(project.getTeam().getId());
        UUID clusterId = request.clusterId().orElse(project.getCluster().getId());
        UUID policyId = request.policyId().orElse(project.getPolicy().getId());

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + teamId));
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + clusterId));
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found with id: " + policyId));

        validateProjectScope(teamId, clusterId, policy);

        project.setTeam(team);
        project.setCluster(cluster);
        project.setPolicy(policy);
        if (request.name().isPresent()) {
            project.setName(request.name().orElse(null));
        }
        if (request.description().isPresent()) {
            project.setDescription(request.description().orElse(null));
        }
        if (request.mlflowExperimentId().isPresent()) {
            project.setMlflowExperimentId(request.mlflowExperimentId().orElse(null));
        }
        if (request.minioPrefix().isPresent()) {
            project.setMinioPrefix(request.minioPrefix().orElse(null));
        }

        return toDto(projectRepository.save(project));
    }

    public void delete(UUID id) {
        projectRepository.delete(getProject(id));
    }

    private void apply(
            Project project,
            UUID teamId,
            UUID clusterId,
            UUID policyId,
            String name,
            String description,
            String mlflowExperimentId,
            String minioPrefix) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + teamId));
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + clusterId));
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found with id: " + policyId));

        validateProjectScope(teamId, clusterId, policy);

        project.setTeam(team);
        project.setCluster(cluster);
        project.setPolicy(policy);
        project.setName(name);
        project.setDescription(description);
        project.setMlflowExperimentId(mlflowExperimentId);
        project.setMinioPrefix(minioPrefix);
    }

    private void validateProjectScope(UUID teamId, UUID clusterId, Policy policy) {
        if (!policy.getCluster().getId().equals(clusterId)) {
            throw new IllegalArgumentException("Policy does not belong to the provided cluster");
        }
        if (!teamClusterRepository.existsByTeam_IdAndCluster_Id(teamId, clusterId)) {
            throw new IllegalArgumentException("Team is not assigned to the provided cluster");
        }
    }

    private Project getProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));
    }

    private ProjectDto toDto(Project project) {
        return new ProjectDto(
                project.getId(),
                project.getTeam().getId(),
                project.getCluster().getId(),
                project.getPolicy().getId(),
                project.getName(),
                project.getDescription(),
                project.getMlflowExperimentId(),
                project.getMinioPrefix(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
