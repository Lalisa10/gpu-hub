package com.trucdnd.gpu_hub_backend.workload_source.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttachSourceRequest(
        @NotNull UUID sourceId,
        @NotBlank String mountPath
) {
}
