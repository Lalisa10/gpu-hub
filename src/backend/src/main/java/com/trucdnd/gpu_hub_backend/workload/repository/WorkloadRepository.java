package com.trucdnd.gpu_hub_backend.workload.repository;

import com.trucdnd.gpu_hub_backend.workload.entity.Workload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkloadRepository extends JpaRepository<Workload, UUID> {
}
