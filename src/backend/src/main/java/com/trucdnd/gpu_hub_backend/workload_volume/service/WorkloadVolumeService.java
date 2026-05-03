package com.trucdnd.gpu_hub_backend.workload_volume.service;

import com.trucdnd.gpu_hub_backend.data_volume.entity.DataVolume;
import com.trucdnd.gpu_hub_backend.data_volume.repository.DataVolumeRepository;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;
import com.trucdnd.gpu_hub_backend.workload.repository.WorkloadRepository;
import com.trucdnd.gpu_hub_backend.workload_volume.dto.AttachVolumeRequest;
import com.trucdnd.gpu_hub_backend.workload_volume.dto.WorkloadVolumeDto;
import com.trucdnd.gpu_hub_backend.workload_volume.entity.WorkloadVolume;
import com.trucdnd.gpu_hub_backend.workload_volume.repository.WorkloadVolumeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkloadVolumeService {

    private final WorkloadVolumeRepository workloadVolumeRepository;
    private final WorkloadRepository workloadRepository;
    private final DataVolumeRepository dataVolumeRepository;

    public List<WorkloadVolumeDto> findByWorkload(UUID workloadId) {
        return workloadVolumeRepository.findByWorkload_Id(workloadId).stream().map(this::toDto).toList();
    }

    public WorkloadVolumeDto attach(UUID workloadId, AttachVolumeRequest request) {
        Workload workload = workloadRepository.findById(workloadId)
                .orElseThrow(() -> new EntityNotFoundException("Workload not found: " + workloadId));
        DataVolume volume = dataVolumeRepository.findById(request.volumeId())
                .orElseThrow(() -> new EntityNotFoundException("DataVolume not found: " + request.volumeId()));

        if (!workload.getCluster().getId().equals(volume.getCluster().getId())) {
            throw new IllegalArgumentException("Workload and volume must belong to the same cluster");
        }
        UUID projectTeamId = workload.getProject().getTeam().getId();
        if (!projectTeamId.equals(volume.getTeam().getId())) {
            throw new IllegalArgumentException(
                    "DataVolume " + volume.getId() + " belongs to team " + volume.getTeam().getId()
                            + " which does not own the workload's project (team " + projectTeamId + ")");
        }
        if (workloadVolumeRepository.existsByWorkload_IdAndVolume_Id(workloadId, volume.getId())) {
            throw new IllegalArgumentException("Volume is already attached to this workload");
        }

        WorkloadVolume binding = WorkloadVolume.builder()
                .workload(workload)
                .volume(volume)
                .mountPath(request.mountPath())
                .build();
        return toDto(workloadVolumeRepository.save(binding));
    }

    @Transactional
    public void detach(UUID workloadId, UUID volumeId) {
        if (!workloadVolumeRepository.existsByWorkload_IdAndVolume_Id(workloadId, volumeId)) {
            throw new EntityNotFoundException("Volume " + volumeId + " is not attached to workload " + workloadId);
        }
        workloadVolumeRepository.deleteByWorkload_IdAndVolume_Id(workloadId, volumeId);
    }

    private WorkloadVolumeDto toDto(WorkloadVolume w) {
        return new WorkloadVolumeDto(
                w.getWorkload().getId(),
                w.getVolume().getId(),
                w.getMountPath()
        );
    }
}
