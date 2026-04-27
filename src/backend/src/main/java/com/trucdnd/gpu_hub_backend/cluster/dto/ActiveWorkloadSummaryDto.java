package com.trucdnd.gpu_hub_backend.cluster.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ActiveWorkloadSummaryDto(
        UUID id,
        String name,
        String workloadType,
        BigDecimal requestedGpu,
        String status,
        OffsetDateTime startedAt
) {}
