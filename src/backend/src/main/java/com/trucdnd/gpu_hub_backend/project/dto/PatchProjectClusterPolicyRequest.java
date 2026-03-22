package com.trucdnd.gpu_hub_backend.project.dto;

import lombok.Builder;

import java.util.Optional;
import java.util.UUID;

@Builder
public record PatchProjectClusterPolicyRequest(
        Optional<UUID> projectId,
        Optional<UUID> clusterId,
        Optional<UUID> policyId
) {
}
