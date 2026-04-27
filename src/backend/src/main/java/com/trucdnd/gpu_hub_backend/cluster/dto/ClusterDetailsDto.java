package com.trucdnd.gpu_hub_backend.cluster.dto;

import java.util.List;
import java.util.UUID;

public record ClusterDetailsDto(
        UUID clusterId,
        String clusterName,
        int gpusTotal,
        int gpusInUse,
        List<NodeInfoDto> nodes,
        List<GpuInfoDto> gpus,
        List<ActiveWorkloadSummaryDto> activeWorkloads
) {}
