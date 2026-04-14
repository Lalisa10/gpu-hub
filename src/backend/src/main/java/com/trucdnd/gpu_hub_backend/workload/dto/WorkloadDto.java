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
        String name,
        BigDecimal requestedGpu,
        BigDecimal requestedCpu,
        BigDecimal requestedCpuLimit,
        Long requestedMemory,
        Long requestedMemoryLimit,
        com.trucdnd.gpu_hub_backend.common.constants.Workload.Status status,
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
