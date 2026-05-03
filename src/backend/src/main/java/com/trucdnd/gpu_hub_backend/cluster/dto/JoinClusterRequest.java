package com.trucdnd.gpu_hub_backend.cluster.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record JoinClusterRequest(
    @NotBlank String name,
    String description,
    String kubeconfigRef,
    String juicefsMetaurl
) { }
