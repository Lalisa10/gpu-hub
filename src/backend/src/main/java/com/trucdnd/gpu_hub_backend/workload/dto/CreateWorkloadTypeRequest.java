package com.trucdnd.gpu_hub_backend.workload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateWorkloadTypeRequest(
        @NotBlank String name,
        @NotBlank String displayName,
        String description,
        BigDecimal defaultGpu,
        BigDecimal defaultCpu,
        Long defaultMemory,
        @NotNull Boolean supportsMultiGpu,
        @NotNull Boolean isService,
        @NotNull Boolean isActive
) {
}
