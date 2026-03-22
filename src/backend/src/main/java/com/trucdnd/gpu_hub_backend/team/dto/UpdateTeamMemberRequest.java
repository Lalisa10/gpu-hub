package com.trucdnd.gpu_hub_backend.team.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record UpdateTeamMemberRequest(
        @NotNull OffsetDateTime joinedAt
) {
}
