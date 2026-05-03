package com.trucdnd.gpu_hub_backend.workload_volume.repository;

import com.trucdnd.gpu_hub_backend.workload_volume.entity.WorkloadVolume;
import com.trucdnd.gpu_hub_backend.workload_volume.entity.WorkloadVolumeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkloadVolumeRepository extends JpaRepository<WorkloadVolume, WorkloadVolumeId> {

    List<WorkloadVolume> findByWorkload_Id(UUID workloadId);

    List<WorkloadVolume> findByVolume_Id(UUID volumeId);

    void deleteByWorkload_IdAndVolume_Id(UUID workloadId, UUID volumeId);

    boolean existsByWorkload_IdAndVolume_Id(UUID workloadId, UUID volumeId);
}
