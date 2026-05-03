package com.trucdnd.gpu_hub_backend.data_source.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.common.constants.DataSource.Status;
import com.trucdnd.gpu_hub_backend.common.constants.DataVolume.VolumeType;
import com.trucdnd.gpu_hub_backend.data_source.dto.CreateDataSourceRequest;
import com.trucdnd.gpu_hub_backend.data_source.dto.DataSourceDto;
import com.trucdnd.gpu_hub_backend.data_source.dto.PatchDataSourceRequest;
import com.trucdnd.gpu_hub_backend.data_source.dto.UpdateDataSourceRequest;
import com.trucdnd.gpu_hub_backend.data_source.entity.DataSource;
import com.trucdnd.gpu_hub_backend.data_source.repository.DataSourceRepository;
import com.trucdnd.gpu_hub_backend.data_volume.entity.DataVolume;
import com.trucdnd.gpu_hub_backend.data_volume.repository.DataVolumeRepository;
import com.trucdnd.gpu_hub_backend.kubernetes.service.BuiltinResourceService;
import com.trucdnd.gpu_hub_backend.team.entity.Team;
import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;
import com.trucdnd.gpu_hub_backend.team.repository.TeamClusterRepository;
import com.trucdnd.gpu_hub_backend.team.repository.TeamMemberRepository;
import com.trucdnd.gpu_hub_backend.team.repository.TeamRepository;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import com.trucdnd.gpu_hub_backend.user.repository.UserRepository;
import com.trucdnd.gpu_hub_backend.workload_volume.repository.WorkloadVolumeRepository;
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
    private final DataVolumeRepository dataVolumeRepository;
    private final ClusterRepository clusterRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamClusterRepository teamClusterRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final WorkloadVolumeRepository workloadVolumeRepository;
    private final BuiltinResourceService builtinResourceService;
    private final DataSourceProvisioner dataSourceProvisioner;

    public List<DataSourceDto> findAll() {
        return dataSourceRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<DataSourceDto> findByVolume(UUID volumeId) {
        return dataSourceRepository.findByVolume_Id(volumeId).stream().map(this::toDto).toList();
    }

    public List<DataSourceDto> findByUser(UUID userId) {
        List<UUID> teamIds = teamMemberRepository.findTeamIdsByUserId(userId);
        if (teamIds.isEmpty()) return List.of();
        return dataSourceRepository.findByVolume_Team_IdIn(teamIds).stream().map(this::toDto).toList();
    }

    public DataSourceDto findById(UUID id) {
        return toDto(getSource(id));
    }

    public String getMigrationJobLogs(UUID id) {
        DataSource source = getSource(id);
        DataVolume volume = source.getVolume();
        Cluster cluster = volume.getCluster();
        TeamCluster teamCluster = teamClusterRepository
                .findByTeam_IdAndCluster_Id(volume.getTeam().getId(), cluster.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "TeamCluster not found for team " + volume.getTeam().getId()
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

        if (cluster.getJuicefsMetaurl() == null || cluster.getJuicefsMetaurl().isBlank()) {
            throw new IllegalStateException("Cluster '" + cluster.getName() + "' has no juicefs_metaurl configured");
        }

        if (builtinResourceService.persistentVolumeClaimExists(cluster, namespace, request.pvcName())) {
            throw new IllegalArgumentException(
                    "PVC '" + request.pvcName() + "' already exists in namespace '" + namespace + "'");
        }

        DataVolume volume = dataVolumeRepository.save(DataVolume.builder()
                .team(team)
                .cluster(cluster)
                .createdBy(createdBy)
                .pvcName(request.pvcName())
                .volumeType(VolumeType.SOURCE)
                .build());

        DataSource source = dataSourceRepository.save(DataSource.builder()
                .createdBy(createdBy)
                .volume(volume)
                .status(Status.FORMATING)
                .bucketUrl(request.bucketUrl())
                .accessKey(request.accessKey())
                .secretKey(request.secretKey())
                .build());

        UUID sourceId = source.getId();
        String pvcName = request.pvcName();
        String sourcePath = request.sourcePath();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                DataSource committed = dataSourceRepository.findById(sourceId).orElse(null);
                if (committed == null) return;
                try {
                    dataSourceProvisioner.provision(committed, namespace, pvcName, sourcePath);
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
        DataVolume volume = source.getVolume();

        if (!workloadVolumeRepository.findByVolume_Id(volume.getId()).isEmpty()) {
            throw new IllegalStateException(
                    "DataSource " + id + " has workloads still mounting its volume; detach them first");
        }

        Cluster cluster = volume.getCluster();
        TeamCluster teamCluster = teamClusterRepository
                .findByTeam_IdAndCluster_Id(volume.getTeam().getId(), cluster.getId())
                .orElse(null);
        if (teamCluster != null) {
            dataSourceProvisioner.teardown(cluster, teamCluster.getNamespace(), id.toString());
        }

        dataSourceRepository.delete(source);
        dataVolumeRepository.delete(volume);
    }

    private DataSource getSource(UUID id) {
        return dataSourceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DataSource not found: " + id));
    }

    public DataSourceDto toDto(DataSource s) {
        DataVolume v = s.getVolume();
        return new DataSourceDto(
                s.getId(),
                v.getCluster().getId(),
                v.getTeam().getId(),
                s.getCreatedBy().getId(),
                v.getId(),
                v.getPvcName(),
                s.getStatus(),
                s.getBucketUrl(),
                s.getAccessKey(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
