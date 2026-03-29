package com.trucdnd.gpu_hub_backend.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateProjectRequest(
        @NotNull UUID teamId,
        @NotNull UUID clusterId,
        @NotNull UUID policyId,
        @NotBlank String name,
        String description,
        String mlflowExperimentId,
        String minioPrefix
) {
}
