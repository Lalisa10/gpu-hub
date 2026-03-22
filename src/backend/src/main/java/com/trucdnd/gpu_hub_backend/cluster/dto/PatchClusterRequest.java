package com.trucdnd.gpu_hub_backend.cluster.dto;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.trucdnd.gpu_hub_backend.common.constants.Cluster;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record PatchClusterRequest(
    Optional<@NotBlank String> name,
    Optional<String> description,
    Optional<@NotBlank String> apiEndpoint,
    Optional<@NotBlank String> kubeconfigRef,
    Optional<Cluster.Status> status
) {}
