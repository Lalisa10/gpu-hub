package com.trucdnd.gpu_hub_backend.team.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.Optional;

@Builder
public record PatchTeamRequest(
        Optional<@NotBlank String> name,
        Optional<String> description
) {
}
