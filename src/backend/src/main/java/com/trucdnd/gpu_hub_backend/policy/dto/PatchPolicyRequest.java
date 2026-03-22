package com.trucdnd.gpu_hub_backend.policy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Builder
public record PatchPolicyRequest(
        Optional<UUID> clusterId,
        Optional<@NotBlank String> name,
        Optional<String> description,
        Optional<Integer> maxPriority,
        Optional<BigDecimal> gpuQuota,
        Optional<BigDecimal> cpuQuota,
        Optional<Long> memoryQuota,
        Optional<BigDecimal> gpuLimit,
        Optional<BigDecimal> cpuLimit,
        Optional<Long> memoryLimit,
        Optional<BigDecimal> overQuotaWeight,
        Optional<String> nodeAffinity,
        Optional<String[]> gpuTypes
) {
}
