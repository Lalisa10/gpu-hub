package com.trucdnd.gpu_hub_backend.cluster.dto;

public record GpuInfoDto(
        int index,
        String nodeName,
        String model,
        String gpuStatus
) {}
