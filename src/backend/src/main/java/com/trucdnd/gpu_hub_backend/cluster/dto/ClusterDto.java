package com.trucdnd.gpu_hub_backend.cluster.dto;

import com.trucdnd.gpu_hub_backend.common.constants.Cluster;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record ClusterDto(
    @NotNull UUID id,
    @NotBlank String name,
    String description,
    @NotBlank String apiEndpoint,
    @NotNull Cluster.Status status,
    @NotNull OffsetDateTime createdAt,
    @NotNull OffsetDateTime updatedAt
    ) {}
