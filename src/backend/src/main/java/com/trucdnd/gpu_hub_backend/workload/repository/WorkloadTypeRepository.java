package com.trucdnd.gpu_hub_backend.workload.repository;

import com.trucdnd.gpu_hub_backend.workload.entity.WorkloadType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkloadTypeRepository extends JpaRepository<WorkloadType, UUID> {
}
