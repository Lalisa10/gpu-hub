package com.trucdnd.gpu_hub_backend.workload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateWorkloadRequest(
        @NotNull UUID projectId,
        @NotNull UUID clusterId,
        @NotNull UUID submittedById,
        @NotNull UUID workloadTypeId,
        @NotBlank String name,
        @NotNull Integer priority,
        @NotNull BigDecimal requestedGpu,
        @NotNull BigDecimal requestedCpu,
        @NotNull Long requestedMemory,
        @NotBlank String status,
        String k8sNamespace,
        String k8sResourceName,
        String k8sResourceKind,
        OffsetDateTime queuedAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String extra
) {
}
