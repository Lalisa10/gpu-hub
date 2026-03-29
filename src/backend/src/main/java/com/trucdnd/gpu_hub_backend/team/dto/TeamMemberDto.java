package com.trucdnd.gpu_hub_backend.team.dto;

import com.trucdnd.gpu_hub_backend.common.constants.Team;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TeamMemberDto(
        UUID userId,
        UUID teamId,
        Team.TeamRole role,
        OffsetDateTime joinedAt
) {
}
