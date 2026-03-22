package com.trucdnd.gpu_hub_backend.team.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TeamClusterDto(
        UUID id,
        UUID teamId,
        UUID clusterId,
        UUID policyId,
        String namespace,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
