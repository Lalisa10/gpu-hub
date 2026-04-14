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
        @NotBlank Type workloadType,
        @NotBlank PriorityClass priorityClass,
        @NotBlank String name,
        @NotNull BigDecimal requestedGpu,
        @NotNull BigDecimal requestedCpu,
        @NotNull BigDecimal requestedCpuLimit,
        @NotNull Long requestedMemory,
        @NotNull Long requestedMemoryLimit,
        String extra
) {
}
