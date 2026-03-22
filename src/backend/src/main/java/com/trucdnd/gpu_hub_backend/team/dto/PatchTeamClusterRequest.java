package com.trucdnd.gpu_hub_backend.team.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.Optional;
import java.util.UUID;

@Builder
public record PatchTeamClusterRequest(
        Optional<UUID> teamId,
        Optional<UUID> clusterId,
        Optional<UUID> policyId,
        Optional<@NotBlank String> namespace
) {
}
