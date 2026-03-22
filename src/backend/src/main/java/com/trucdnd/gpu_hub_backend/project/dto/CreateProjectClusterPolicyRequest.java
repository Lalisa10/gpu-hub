package com.trucdnd.gpu_hub_backend.project.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateProjectClusterPolicyRequest(
        @NotNull UUID projectId,
        @NotNull UUID clusterId,
        @NotNull UUID policyId
) {
}
