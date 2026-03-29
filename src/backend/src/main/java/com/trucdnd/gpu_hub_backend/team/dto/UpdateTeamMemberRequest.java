package com.trucdnd.gpu_hub_backend.team.dto;

import com.trucdnd.gpu_hub_backend.common.constants.Team;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record UpdateTeamMemberRequest(
        @NotNull Team.TeamRole role,
        @NotNull OffsetDateTime joinedAt
) {
}
