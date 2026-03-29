package com.trucdnd.gpu_hub_backend.team.dto;

import com.trucdnd.gpu_hub_backend.common.constants.Team;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateTeamMemberRequest(
        @NotNull UUID userId,
        @NotNull UUID teamId,
        Team.TeamRole role,
        OffsetDateTime joinedAt
) {
}
