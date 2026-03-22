package com.trucdnd.gpu_hub_backend.team.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TeamDto(
        UUID id,
        String name,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
