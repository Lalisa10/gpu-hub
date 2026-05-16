package com.trucdnd.gpu_hub_backend.cluster.mapper;

import com.trucdnd.gpu_hub_backend.cluster.dto.ClusterDto;
import com.trucdnd.gpu_hub_backend.cluster.dto.JoinClusterRequest;
import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;

public class ClusterMapper {
    private ClusterMapper() {

    }

    public static ClusterDto toDto(Cluster cluster) {
        return ClusterDto.builder()
                .id(cluster.getId())
                .name(cluster.getName())
                .description(cluster.getDescription())
                .status(cluster.getStatus())
                .createdAt(cluster.getCreatedAt())
                .updatedAt(cluster.getUpdatedAt())
                .build();
    }

    public static Cluster toCluster(JoinClusterRequest request) {
        return Cluster.builder()
                .name(request.name())
                .description(request.description())
                .kubeconfigRef(request.kubeconfigRef())
                .build();
    }
}
