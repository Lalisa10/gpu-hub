package com.trucdnd.gpu_hub_backend.team.dto;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.Optional;

@Builder
public record PatchTeamMemberRequest(
        Optional<OffsetDateTime> joinedAt
) {
}
