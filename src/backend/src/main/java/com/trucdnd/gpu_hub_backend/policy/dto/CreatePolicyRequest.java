package com.trucdnd.gpu_hub_backend.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePolicyRequest(
        @NotNull UUID clusterId,
        @NotBlank String name,
        String description,
        @NotNull Integer maxPriority,
        BigDecimal gpuQuota,
        BigDecimal cpuQuota,
        Long memoryQuota,
        BigDecimal gpuLimit,
        BigDecimal cpuLimit,
        Long memoryLimit,
        @NotNull BigDecimal overQuotaWeight,
        String nodeAffinity,
        String[] gpuTypes
) {
}
