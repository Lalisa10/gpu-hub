package com.trucdnd.gpu_hub_backend.team.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.common.security.UserPrincipal;
import com.trucdnd.gpu_hub_backend.data_source.config.JuicefsProperties;
import com.trucdnd.gpu_hub_backend.kubernetes.service.BuiltinResourceService;
import com.trucdnd.gpu_hub_backend.kubernetes.service.QueueService;
import com.trucdnd.gpu_hub_backend.kubernetes.util.K8sLabels;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
    private final JuicefsProperties juicefsProperties;

    public List<TeamClusterDto> findAll() {
        return teamClusterRepository.findAll().stream().map(this::toDto).toList();
    }

    public TeamClusterDto findById(UUID id) {
        return toDto(getTeamCluster(id));
    }

    public TeamClusterDto create(CreateTeamClusterRequest request) {
        TeamCluster teamCluster = new TeamCluster();
        apply(teamCluster, request.teamId(), request.clusterId(), request.policyId());
        teamCluster.setPvcSize(request.pvcSize() != null && !request.pvcSize().isBlank()
                ? request.pvcSize()
                : juicefsProperties.getPvcSize());

        kubernetesService.createNamespace(teamCluster.getCluster(), teamCluster.getNamespace());
        queueService.create(teamCluster.getCluster(), queueSpecBuilder.buildTeamQueue(teamCluster));

        TeamCluster saved = teamClusterRepository.save(teamCluster);
        createTeamPvc(saved);
        return toDto(saved);
    }

    public TeamClusterDto update(UUID id, UpdateTeamClusterRequest request) {
        TeamCluster teamCluster = getTeamCluster(id);
        apply(teamCluster, request.teamId(), request.clusterId(), request.policyId());
        return toDto(teamClusterRepository.save(teamCluster));
    }


    public void delete(UUID id) {
        TeamCluster teamCluster = getTeamCluster(id);
        Cluster cluster = clusterRepository.findById(teamCluster.getCluster().getId())
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found"));

        kubernetesService.deletePersistentVolumeClaimsByLabel(
                cluster, teamCluster.getNamespace(),
                K8sLabels.TEAM_CLUSTER_ID, teamCluster.getId().toString());

        // Drop the team-level KAI queue (cluster-scoped, so it lives outside the namespace and
        // would otherwise be orphaned). 404-tolerant via CustomResourceService.delete.
        queueService.delete(cluster, null, queueSpecBuilder.buildTeamQueueName(teamCluster));

        teamClusterRepository.delete(teamCluster);
        kubernetesService.deleteNamespace(cluster, teamCluster.getNamespace());
    }

    private void apply(TeamCluster target, UUID teamId, UUID clusterId, UUID policyId) {
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
        target.setNamespace(buildNamespace(team.getName()));
    }

    private void createTeamPvc(TeamCluster teamCluster) {
        String pvcName = buildPvcName(teamCluster.getTeam().getName());
        if (kubernetesService.persistentVolumeClaimExists(
                teamCluster.getCluster(), teamCluster.getNamespace(), pvcName)) {
            return;
        }
        Map<String, String> labels = K8sLabels.standard(
                K8sLabels.OWNER_TEAM_PVC,
                teamCluster.getTeam().getName(),
                teamCluster.getCluster().getName(),
                currentUsername());
        labels.put(K8sLabels.TEAM_CLUSTER_ID, teamCluster.getId().toString());

        kubernetesService.createPersistentVolumeClaim(
                teamCluster.getCluster(),
                teamCluster.getNamespace(),
                pvcName,
                juicefsProperties.getStorageClass(),
                teamCluster.getPvcSize(),
                List.of("ReadWriteMany"),
                labels);
    }

    /** Generates the K8s namespace: {@code gpuhub-team-<sanitized-team-name>}. */
    public static String buildNamespace(String teamName) {
        String sanitized = sanitizeTeamName(teamName, 63 - "gpuhub-team-".length());
        return "gpuhub-team-" + sanitized;
    }

    /** Generates the team PVC name: {@code gpuhub-team-<sanitized>-data}. */
    public static String buildPvcName(String teamName) {
        String suffix = "-data";
        int budget = 63 - "gpuhub-team-".length() - suffix.length();
        return "gpuhub-team-" + sanitizeTeamName(teamName, budget) + suffix;
    }

    private static String sanitizeTeamName(String teamName, int maxLength) {
        String sanitized = teamName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (sanitized.length() > maxLength) sanitized = sanitized.substring(0, maxLength);
        return sanitized.replaceAll("-+$", "");
    }

    private static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUsername();
        }
        return "system";
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
                entity.getPvcSize(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
