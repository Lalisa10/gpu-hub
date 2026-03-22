package com.trucdnd.gpu_hub_backend.team.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTeamRequest(
        @NotBlank String name,
        String description
) {
}
