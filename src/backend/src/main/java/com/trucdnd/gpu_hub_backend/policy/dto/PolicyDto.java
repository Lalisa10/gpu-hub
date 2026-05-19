package com.trucdnd.gpu_hub_backend.policy.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PolicyDto(
        UUID id,
        UUID clusterId,
        String name,
        String description,
        BigDecimal gpuQuota,
        BigDecimal cpuQuota,
        Long memoryQuota,
        BigDecimal gpuLimit,
        BigDecimal cpuLimit,
        Long memoryLimit,
        Integer gpuOverQuotaWeight,
        Integer cpuOverQuotaWeight,
        Integer memoryOverQuotaWeight,
        List<String> nodePool,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
