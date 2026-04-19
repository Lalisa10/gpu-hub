package com.trucdnd.gpu_hub_backend.workload.dto;

import com.trucdnd.gpu_hub_backend.common.constants.Workload.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateWorkloadRequest(
        @NotNull UUID projectId,
        @NotNull UUID clusterId,
        @NotNull UUID submittedById,
        @NotNull Type workloadType,
        @NotBlank String name,
        @NotBlank String image,
        @NotNull BigDecimal requestedGpu,
        @NotNull BigDecimal requestedCpu,
        @NotNull Long requestedMemory,
        String extra
) {
}
