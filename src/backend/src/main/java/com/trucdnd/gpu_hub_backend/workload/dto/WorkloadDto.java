package com.trucdnd.gpu_hub_backend.workload.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkloadDto(
        UUID id,
        UUID projectId,
        UUID clusterId,
        UUID submittedById,
        com.trucdnd.gpu_hub_backend.common.constants.Workload.Type workloadType,
        com.trucdnd.gpu_hub_backend.common.constants.Workload.PriorityClass priorityClass,
        String image,
        String name,
        BigDecimal requestedGpu,
        BigDecimal requestedCpu,
        Long requestedMemory,
        com.trucdnd.gpu_hub_backend.common.constants.Workload.Status status,
        OffsetDateTime queuedAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String extra,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
