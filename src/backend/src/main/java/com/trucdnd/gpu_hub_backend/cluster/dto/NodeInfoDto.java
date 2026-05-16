package com.trucdnd.gpu_hub_backend.cluster.dto;

public record NodeInfoDto(
        String name,
        String status,                  // Ready | NotReady | Pressure
        long cpuCapacityMillis,
        long cpuAllocatableMillis,
        long cpuRequestMillis,
        long cpuLimitMillis,
        long ramCapacityBytes,
        long ramAllocatableBytes,
        long ramRequestBytes,
        long ramLimitBytes,
        int gpuTotal,
        int gpuAllocated,
        String gpuModel
) {}
