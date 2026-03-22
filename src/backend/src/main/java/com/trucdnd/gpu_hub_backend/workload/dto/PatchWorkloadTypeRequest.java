package com.trucdnd.gpu_hub_backend.workload.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Optional;

@Builder
public record PatchWorkloadTypeRequest(
        Optional<@NotBlank String> name,
        Optional<@NotBlank String> displayName,
        Optional<String> description,
        Optional<BigDecimal> defaultGpu,
        Optional<BigDecimal> defaultCpu,
        Optional<Long> defaultMemory,
        Optional<Boolean> supportsMultiGpu,
        Optional<Boolean> isService,
        Optional<Boolean> isActive
) {
}
