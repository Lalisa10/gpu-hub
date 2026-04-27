package com.trucdnd.gpu_hub_backend.workload.repository;

import com.trucdnd.gpu_hub_backend.common.constants.Workload.Status;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkloadRepository extends JpaRepository<Workload, UUID> {

    @Query("SELECT w FROM Workload w WHERE w.cluster.id = :clusterId AND w.status = :status")
    List<Workload> findByClusterIdAndStatus(
            @Param("clusterId") UUID clusterId,
            @Param("status") Status status
    );
}
