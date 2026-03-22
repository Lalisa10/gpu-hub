package com.trucdnd.gpu_hub_backend.cluster.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;

@Repository
public interface ClusterRepository extends JpaRepository<Cluster, UUID> {
    
}
