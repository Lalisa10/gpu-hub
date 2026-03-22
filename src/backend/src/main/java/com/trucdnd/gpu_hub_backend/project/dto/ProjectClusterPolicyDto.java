package com.trucdnd.gpu_hub_backend.project.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectClusterPolicyDto(
        UUID id,
        UUID projectId,
        UUID clusterId,
        UUID policyId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
