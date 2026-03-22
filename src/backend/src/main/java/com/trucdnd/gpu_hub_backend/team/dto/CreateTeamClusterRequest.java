package com.trucdnd.gpu_hub_backend.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTeamClusterRequest(
        @NotNull UUID teamId,
        @NotNull UUID clusterId,
        @NotNull UUID policyId,
        @NotBlank String namespace
) {
}
