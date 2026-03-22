package com.trucdnd.gpu_hub_backend.team.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTeamRequest(
        @NotBlank String name,
        String description
) {
}
