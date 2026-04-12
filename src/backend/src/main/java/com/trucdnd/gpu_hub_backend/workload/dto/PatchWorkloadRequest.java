package com.trucdnd.gpu_hub_backend.workload.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Builder
public record PatchWorkloadRequest(
        Optional<UUID> projectId,
        Optional<UUID> clusterId,
        Optional<UUID> submittedById,
        Optional<UUID> workloadTypeId,
        Optional<@NotBlank String> name,
        Optional<BigDecimal> requestedGpu,
        Optional<BigDecimal> requestedCpu,
        Optional<Long> requestedMemory,
        Optional<@NotBlank String> status,
        Optional<String> k8sNamespace,
        Optional<String> k8sResourceName,
        Optional<String> k8sResourceKind,
        Optional<OffsetDateTime> queuedAt,
        Optional<OffsetDateTime> startedAt,
        Optional<OffsetDateTime> finishedAt,
        Optional<String> extra
) {
}
