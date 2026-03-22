package com.trucdnd.gpu_hub_backend.policy.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PolicyDto(
        UUID id,
        UUID clusterId,
        String name,
        String description,
        Integer maxPriority,
        BigDecimal gpuQuota,
        BigDecimal cpuQuota,
        Long memoryQuota,
        BigDecimal gpuLimit,
        BigDecimal cpuLimit,
        Long memoryLimit,
        BigDecimal overQuotaWeight,
        String nodeAffinity,
        String[] gpuTypes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
