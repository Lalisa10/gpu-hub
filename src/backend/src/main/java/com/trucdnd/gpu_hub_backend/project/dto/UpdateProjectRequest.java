package com.trucdnd.gpu_hub_backend.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateProjectRequest(
        @NotNull UUID teamId,
        @NotBlank String name,
        String description,
        String mlflowExperimentId,
        String minioPrefix
) {
}
