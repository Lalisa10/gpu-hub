package com.trucdnd.gpu_hub_backend.workload.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkloadTypeDto(
        UUID id,
        String name,
        String displayName,
        String description,
        BigDecimal defaultGpu,
        BigDecimal defaultCpu,
        Long defaultMemory,
        Boolean supportsMultiGpu,
        Boolean isService,
        Boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
