package com.trucdnd.gpu_hub_backend.team.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateTeamClusterRequest(
        @NotNull UUID teamId,
        @NotNull UUID clusterId,
        @NotNull UUID policyId
) {
}
