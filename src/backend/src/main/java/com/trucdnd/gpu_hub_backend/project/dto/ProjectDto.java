package com.trucdnd.gpu_hub_backend.project.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectDto(
        UUID id,
        UUID teamId,
        String name,
        String description,
        String mlflowExperimentId,
        String minioPrefix,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
