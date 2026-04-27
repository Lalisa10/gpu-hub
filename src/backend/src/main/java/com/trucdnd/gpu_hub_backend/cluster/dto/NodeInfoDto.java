package com.trucdnd.gpu_hub_backend.cluster.dto;

public record NodeInfoDto(
        String name,
        boolean ready,
        long cpuCapacityMillis,
        long cpuAllocatableMillis,
        long ramCapacityBytes,
        long ramAllocatableBytes,
        int gpuTotal,
        String gpuModel
) {}
