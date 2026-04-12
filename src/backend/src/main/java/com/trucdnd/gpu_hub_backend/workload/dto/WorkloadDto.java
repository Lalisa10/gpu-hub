package com.trucdnd.gpu_hub_backend.workload.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkloadDto(
        UUID id,
        UUID projectId,
        UUID clusterId,
        UUID submittedById,
        UUID workloadTypeId,
        String name,
        BigDecimal requestedGpu,
        BigDecimal requestedCpu,
        Long requestedMemory,
        String status,
        String k8sNamespace,
        String k8sResourceName,
        String k8sResourceKind,
        OffsetDateTime queuedAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String extra,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
