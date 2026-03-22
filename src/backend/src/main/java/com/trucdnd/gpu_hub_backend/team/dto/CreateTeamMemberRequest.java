package com.trucdnd.gpu_hub_backend.team.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateTeamMemberRequest(
        @NotNull UUID userId,
        @NotNull UUID teamId,
        OffsetDateTime joinedAt
) {
}
