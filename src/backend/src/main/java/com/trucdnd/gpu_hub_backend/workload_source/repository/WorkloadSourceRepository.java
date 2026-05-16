package com.trucdnd.gpu_hub_backend.workload_source.repository;

import com.trucdnd.gpu_hub_backend.workload_source.entity.WorkloadSource;
import com.trucdnd.gpu_hub_backend.workload_source.entity.WorkloadSourceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkloadSourceRepository extends JpaRepository<WorkloadSource, WorkloadSourceId> {

    List<WorkloadSource> findByWorkload_Id(UUID workloadId);

    List<WorkloadSource> findBySource_Id(UUID sourceId);

    boolean existsByWorkload_IdAndSource_Id(UUID workloadId, UUID sourceId);

    void deleteByWorkload_IdAndSource_Id(UUID workloadId, UUID sourceId);
}
