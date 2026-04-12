package com.trucdnd.gpu_hub_backend.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CreatePolicyRequest(
        @NotNull UUID clusterId,
        @NotBlank String name,
        String description,
        @NotNull Integer priority,
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
        String[] gpuTypes
) {
}
