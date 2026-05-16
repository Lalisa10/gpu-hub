package com.trucdnd.gpu_hub_backend.data_source.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.common.constants.DataSource.Status;
import com.trucdnd.gpu_hub_backend.data_source.dto.CreateDataSourceRequest;
import com.trucdnd.gpu_hub_backend.data_source.dto.DataSourceDto;
import com.trucdnd.gpu_hub_backend.data_source.dto.PatchDataSourceRequest;
import com.trucdnd.gpu_hub_backend.data_source.dto.UpdateDataSourceRequest;
import com.trucdnd.gpu_hub_backend.data_source.entity.DataSource;
import com.trucdnd.gpu_hub_backend.data_source.repository.DataSourceRepository;
import com.trucdnd.gpu_hub_backend.kubernetes.service.BuiltinResourceService;
import com.trucdnd.gpu_hub_backend.team.entity.Team;
import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;
import com.trucdnd.gpu_hub_backend.team.repository.TeamClusterRepository;
import com.trucdnd.gpu_hub_backend.team.repository.TeamMemberRepository;
import com.trucdnd.gpu_hub_backend.team.repository.TeamRepository;
import com.trucdnd.gpu_hub_backend.team.service.TeamClusterService;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import com.trucdnd.gpu_hub_backend.user.repository.UserRepository;
import com.trucdnd.gpu_hub_backend.workload_source.repository.WorkloadSourceRepository;
import io.fabric8.kubernetes.api.model.Pod;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    private final ClusterRepository clusterRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamClusterRepository teamClusterRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final WorkloadSourceRepository workloadSourceRepository;
    private final BuiltinResourceService builtinResourceService;
    private final DataSourceProvisioner dataSourceProvisioner;

    public List<DataSourceDto> findAll() {
        return dataSourceRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<DataSourceDto> findByTeam(UUID teamId) {
        return dataSourceRepository.findByTeam_Id(teamId).stream().map(this::toDto).toList();
    }

    public List<DataSourceDto> findByUser(UUID userId) {
        List<UUID> teamIds = teamMemberRepository.findTeamIdsByUserId(userId);
        if (teamIds.isEmpty()) return List.of();
        return dataSourceRepository.findByTeam_IdIn(teamIds).stream().map(this::toDto).toList();
    }

    public DataSourceDto findById(UUID id) {
        return toDto(getSource(id));
    }

    public String getMigrationJobLogs(UUID id) {
        DataSource source = getSource(id);
        Cluster cluster = source.getCluster();
        TeamCluster teamCluster = teamClusterRepository
                .findByTeam_IdAndCluster_Id(source.getTeam().getId(), cluster.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "TeamCluster not found for team " + source.getTeam().getId()
                                + " on cluster " + cluster.getId()));
        List<Pod> pods = builtinResourceService.listPodsByLabel(
                cluster,
                teamCluster.getNamespace(),
                Map.of(JuicefsResourceBuilder.DATA_SOURCE_ID_LABEL, id.toString()));
        if (pods.isEmpty()) return "";
        String podName = pods.get(0).getMetadata().getName();
        return builtinResourceService.getPodLogs(cluster, teamCluster.getNamespace(), podName);
    }

    @Transactional
    public DataSourceDto create(CreateDataSourceRequest request) {
        Cluster cluster = clusterRepository.findById(request.clusterId())
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found: " + request.clusterId()));
        User createdBy = userRepository.findById(request.createdById())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.createdById()));
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new EntityNotFoundException("Team not found: " + request.teamId()));

        TeamCluster teamCluster = teamClusterRepository
                .findByTeam_IdAndCluster_Id(team.getId(), cluster.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Team " + team.getId() + " is not assigned to cluster " + cluster.getId()));
        String namespace = teamCluster.getNamespace();
        String teamPvcName = TeamClusterService.buildPvcName(team.getName());

        if (dataSourceRepository.existsByTeam_IdAndName(team.getId(), request.name())) {
            throw new IllegalArgumentException(
                    "Data source name '" + request.name() + "' already used by team " + team.getName());
        }

        if (!builtinResourceService.persistentVolumeClaimExists(cluster, namespace, teamPvcName)) {
            throw new IllegalStateException("Team PVC '" + teamPvcName + "' missing in namespace '"
                    + namespace + "' — recreate the team-cluster assignment");
        }

        String folderName = sanitizeFolderName(request.name());

        DataSource source = dataSourceRepository.save(DataSource.builder()
                .createdBy(createdBy)
                .team(team)
                .cluster(cluster)
                .name(request.name())
                .folderName(folderName)
                .status(Status.FORMATING)
                .bucketUrl(request.bucketUrl())
                .accessKey(request.accessKey())
                .secretKey(request.secretKey())
                .build());

        UUID sourceId = source.getId();
        String sourcePath = request.sourcePath();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                DataSource committed = dataSourceRepository.findById(sourceId).orElse(null);
                if (committed == null) return;
                try {
                    dataSourceProvisioner.provision(committed, namespace, teamPvcName, sourcePath);
                } catch (RuntimeException e) {
                    // already logged; row stays FORMATING for operator visibility
                }
            }
        });

        return toDto(source);
    }

    public DataSourceDto update(UUID id, UpdateDataSourceRequest request) {
        DataSource source = getSource(id);
        source.setStatus(request.status());
        source.setBucketUrl(request.bucketUrl());
        source.setAccessKey(request.accessKey());
        source.setSecretKey(request.secretKey());
        return toDto(dataSourceRepository.save(source));
    }

    public DataSourceDto patch(UUID id, PatchDataSourceRequest request) {
        DataSource source = getSource(id);
        if (request.status().isPresent()) {
            source.setStatus(request.status().orElse(null));
        }
        if (request.bucketUrl().isPresent()) {
            source.setBucketUrl(request.bucketUrl().orElse(null));
        }
        if (request.accessKey().isPresent()) {
            source.setAccessKey(request.accessKey().orElse(null));
        }
        if (request.secretKey().isPresent()) {
            source.setSecretKey(request.secretKey().orElse(null));
        }
        return toDto(dataSourceRepository.save(source));
    }

    @Transactional
    public void delete(UUID id) {
        DataSource source = getSource(id);

        if (!workloadSourceRepository.findBySource_Id(source.getId()).isEmpty()) {
            throw new IllegalStateException(
                    "DataSource " + id + " is still attached to workloads; detach them first");
        }

        Cluster cluster = source.getCluster();
        TeamCluster teamCluster = teamClusterRepository
                .findByTeam_IdAndCluster_Id(source.getTeam().getId(), cluster.getId())
                .orElse(null);
        if (teamCluster != null) {
            dataSourceProvisioner.teardown(cluster, teamCluster.getNamespace(), id.toString());
        }

        dataSourceRepository.delete(source);
    }

    private DataSource getSource(UUID id) {
        return dataSourceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DataSource not found: " + id));
    }

    public DataSourceDto toDto(DataSource s) {
        return new DataSourceDto(
                s.getId(),
                s.getCluster().getId(),
                s.getTeam().getId(),
                s.getCreatedBy().getId(),
                s.getName(),
                s.getFolderName(),
                s.getStatus(),
                s.getBucketUrl(),
                s.getAccessKey(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }

    static String sanitizeFolderName(String name) {
        String sanitized = name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("data source name '" + name + "' sanitizes to empty");
        }
        if (sanitized.length() > 53) sanitized = sanitized.substring(0, 53).replaceAll("-+$", "");
        return sanitized;
    }
}
