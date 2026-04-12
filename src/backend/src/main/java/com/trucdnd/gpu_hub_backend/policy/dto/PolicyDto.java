package com.trucdnd.gpu_hub_backend.policy.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record PolicyDto(
        UUID id,
        UUID clusterId,
        String name,
        String description,
        Integer priority,
        BigDecimal gpuQuota,
        BigDecimal cpuQuota,
        Long memoryQuota,
        BigDecimal gpuLimit,
        BigDecimal cpuLimit,
        Long memoryLimit,
        Integer gpuOverQuotaWeight,
        Integer cpuOverQuotaWeight,
        Integer memoryOverQuotaWeight,
        Map<String, Object> nodeAffinity,
        String[] gpuTypes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
