package com.trucdnd.gpu_hub_backend.cluster.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Builder
public record JoinClusterRequest(
    @NotBlank String name,
    String description,
    String apiEndpoint,
    String kubeconfigRef
) { }
