package com.trucdnd.gpu_hub_backend.team.dto;

import com.trucdnd.gpu_hub_backend.common.constants.Team;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.Optional;

@Builder
public record PatchTeamMemberRequest(
        Optional<Team.TeamRole> role,
        Optional<OffsetDateTime> joinedAt
) {
}
