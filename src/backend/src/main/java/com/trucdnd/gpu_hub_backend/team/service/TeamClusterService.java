package com.trucdnd.gpu_hub_backend.team.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.kubernetes.service.BuiltinResourceService;
import com.trucdnd.gpu_hub_backend.kubernetes.service.QueueService;
import com.trucdnd.gpu_hub_backend.policy.entity.Policy;
import com.trucdnd.gpu_hub_backend.policy.repository.PolicyRepository;
import com.trucdnd.gpu_hub_backend.policy.service.QueueSpecBuilder;
import com.trucdnd.gpu_hub_backend.team.dto.CreateTeamClusterRequest;
import com.trucdnd.gpu_hub_backend.team.dto.TeamClusterDto;
import com.trucdnd.gpu_hub_backend.team.dto.UpdateTeamClusterRequest;
import com.trucdnd.gpu_hub_backend.team.entity.Team;
import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;
import com.trucdnd.gpu_hub_backend.team.repository.TeamClusterRepository;
import com.trucdnd.gpu_hub_backend.team.repository.TeamRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamClusterService {
    private final TeamClusterRepository teamClusterRepository;
    private final TeamRepository teamRepository;
    private final ClusterRepository clusterRepository;
    private final PolicyRepository policyRepository;
    private final BuiltinResourceService kubernetesService;
    private final QueueService queueService;
    private final QueueSpecBuilder queueSpecBuilder;

    public List<TeamClusterDto> findAll() {
        return teamClusterRepository.findAll().stream().map(this::toDto).toList();
    }

    public TeamClusterDto findById(UUID id) {
        return toDto(getTeamCluster(id));
    }

    public TeamClusterDto create(CreateTeamClusterRequest request) {
        TeamCluster teamCluster = new TeamCluster();
        apply(teamCluster, request.teamId(), request.clusterId(), request.policyId(), request.namespace());
        //kubernetesService.createNamespace(cluster, request.namespace());
        queueService.create(teamCluster.getCluster(), queueSpecBuilder.buildTeamQueue(teamCluster));

        return toDto(teamClusterRepository.save(teamCluster));
    }

    public TeamClusterDto update(UUID id, UpdateTeamClusterRequest request) {
        TeamCluster teamCluster = getTeamCluster(id);
        apply(teamCluster, request.teamId(), request.clusterId(), request.policyId(), teamCluster.getNamespace());
        return toDto(teamClusterRepository.save(teamCluster));
    }


    public void delete(UUID id) {
        TeamCluster teamCluster = getTeamCluster(id);

        teamClusterRepository.delete(teamCluster);

        Cluster cluster = clusterRepository.findById(teamCluster.getCluster().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Cluster not found"));

        kubernetesService.deleteNamespace(cluster, teamCluster.getNamespace());
    }

    private void apply(TeamCluster target, UUID teamId, UUID clusterId, UUID policyId, String namespace) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + teamId));
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + clusterId));
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found with id: " + policyId));

        if (!policy.getCluster().getId().equals(clusterId)) {
            throw new IllegalArgumentException("Policy does not belong to the provided cluster");
        }

        target.setTeam(team);
        target.setCluster(cluster);
        target.setPolicy(policy);
        String normalizedNamespace = namespace == null ? "" : namespace.trim();
        if (!isValidKubernetesName(normalizedNamespace)) {
            throw new IllegalArgumentException(
                    "Invalid namespace name '" + normalizedNamespace + "'. Namespace must match Kubernetes DNS-1123 label format: lowercase alphanumeric or '-', must start/end with alphanumeric, max 63 chars."
            );
        }
        target.setNamespace(normalizedNamespace);
    }

    private TeamCluster getTeamCluster(UUID id) {
        return teamClusterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TeamCluster not found with id: " + id));
    }

    private TeamClusterDto toDto(TeamCluster entity) {
        return new TeamClusterDto(
                entity.getId(),
                entity.getTeam().getId(),
                entity.getCluster().getId(),
                entity.getPolicy().getId(),
                entity.getNamespace(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private boolean isValidKubernetesName(String value) {
        return value != null
                && !value.isBlank()
                && value.length() <= 63
                && value.matches("^[a-z0-9]([-a-z0-9]*[a-z0-9])?$");
    }
}
