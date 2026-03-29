package com.trucdnd.gpu_hub_backend.team.repository;

import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TeamClusterRepository extends JpaRepository<TeamCluster, UUID> {
    boolean existsByTeam_IdAndCluster_Id(UUID teamId, UUID clusterId);
}
