package com.trucdnd.gpu_hub_backend.team.repository;

import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamClusterRepository extends JpaRepository<TeamCluster, UUID> {
    boolean existsByTeam_IdAndCluster_Id(UUID teamId, UUID clusterId);
    List<TeamCluster> findByPolicy_Id(UUID policyId);
    Optional<TeamCluster> findByTeam_IdAndCluster_Id(UUID teamId, UUID clusterId);
}
