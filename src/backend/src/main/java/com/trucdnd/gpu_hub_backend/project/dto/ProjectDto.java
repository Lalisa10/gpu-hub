package com.trucdnd.gpu_hub_backend.project.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectDto(
        UUID id,
        UUID teamId,
        UUID clusterId,
        UUID policyId,
        String name,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
