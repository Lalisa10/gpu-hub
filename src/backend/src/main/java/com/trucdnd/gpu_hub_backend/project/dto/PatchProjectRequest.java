package com.trucdnd.gpu_hub_backend.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.Optional;
import java.util.UUID;

@Builder
public record PatchProjectRequest(
        Optional<UUID> teamId,
        Optional<UUID> clusterId,
        Optional<UUID> policyId,
        Optional<@NotBlank String> name,
        Optional<String> description,
        Optional<String> mlflowExperimentId,
        Optional<String> minioPrefix
) {
}
