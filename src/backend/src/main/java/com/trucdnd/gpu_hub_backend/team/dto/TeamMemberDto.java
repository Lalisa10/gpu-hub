package com.trucdnd.gpu_hub_backend.team.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TeamMemberDto(
        UUID userId,
        UUID teamId,
        OffsetDateTime joinedAt
) {
}
