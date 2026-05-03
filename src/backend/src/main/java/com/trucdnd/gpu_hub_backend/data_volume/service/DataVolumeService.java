package com.trucdnd.gpu_hub_backend.data_volume.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.data_volume.dto.CreateDataVolumeRequest;
import com.trucdnd.gpu_hub_backend.data_volume.dto.DataVolumeDto;
import com.trucdnd.gpu_hub_backend.data_volume.dto.PatchDataVolumeRequest;
import com.trucdnd.gpu_hub_backend.data_volume.dto.UpdateDataVolumeRequest;
import com.trucdnd.gpu_hub_backend.data_volume.entity.DataVolume;
import com.trucdnd.gpu_hub_backend.data_volume.repository.DataVolumeRepository;
import com.trucdnd.gpu_hub_backend.team.entity.Team;
import com.trucdnd.gpu_hub_backend.team.repository.TeamClusterRepository;
import com.trucdnd.gpu_hub_backend.team.repository.TeamMemberRepository;
import com.trucdnd.gpu_hub_backend.team.repository.TeamRepository;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import com.trucdnd.gpu_hub_backend.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DataVolumeService {

    private final DataVolumeRepository dataVolumeRepository;
    private final TeamRepository teamRepository;
    private final ClusterRepository clusterRepository;
    private final UserRepository userRepository;
    private final TeamClusterRepository teamClusterRepository;
    private final TeamMemberRepository teamMemberRepository;

    public List<DataVolumeDto> findAll() {
        return dataVolumeRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<DataVolumeDto> findByTeam(UUID teamId) {
        return dataVolumeRepository.findByTeam_Id(teamId).stream().map(this::toDto).toList();
    }

    public List<DataVolumeDto> findByUser(UUID userId) {
        List<UUID> teamIds = teamMemberRepository.findTeamIdsByUserId(userId);
        if (teamIds.isEmpty()) return List.of();
        return dataVolumeRepository.findByTeam_IdIn(teamIds).stream().map(this::toDto).toList();
    }

    public DataVolumeDto findById(UUID id) {
        return toDto(getVolume(id));
    }

    public DataVolumeDto create(CreateDataVolumeRequest request) {
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new EntityNotFoundException("Team not found: " + request.teamId()));
        Cluster cluster = clusterRepository.findById(request.clusterId())
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found: " + request.clusterId()));
        User createdBy = userRepository.findById(request.createdById())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.createdById()));

        if (!teamClusterRepository.existsByTeam_IdAndCluster_Id(team.getId(), cluster.getId())) {
            throw new IllegalArgumentException("Team is not assigned to the provided cluster");
        }

        DataVolume volume = DataVolume.builder()
                .team(team)
                .cluster(cluster)
                .createdBy(createdBy)
                .pvcName(request.pvcName())
                .volumeType(request.volumeType())
                .build();
        return toDto(dataVolumeRepository.save(volume));
    }

    public DataVolumeDto update(UUID id, UpdateDataVolumeRequest request) {
        DataVolume volume = getVolume(id);
        volume.setPvcName(request.pvcName());
        volume.setVolumeType(request.volumeType());
        return toDto(dataVolumeRepository.save(volume));
    }

    public DataVolumeDto patch(UUID id, PatchDataVolumeRequest request) {
        DataVolume volume = getVolume(id);
        if (request.pvcName().isPresent()) {
            volume.setPvcName(request.pvcName().orElse(null));
        }
        if (request.volumeType().isPresent()) {
            volume.setVolumeType(request.volumeType().orElse(null));
        }
        return toDto(dataVolumeRepository.save(volume));
    }

    public void delete(UUID id) {
        dataVolumeRepository.delete(getVolume(id));
    }

    private DataVolume getVolume(UUID id) {
        return dataVolumeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DataVolume not found: " + id));
    }

    private DataVolumeDto toDto(DataVolume v) {
        return new DataVolumeDto(
                v.getId(),
                v.getTeam().getId(),
                v.getCluster().getId(),
                v.getCreatedBy().getId(),
                v.getPvcName(),
                v.getVolumeType(),
                v.getCreatedAt(),
                v.getUpdatedAt()
        );
    }
}
