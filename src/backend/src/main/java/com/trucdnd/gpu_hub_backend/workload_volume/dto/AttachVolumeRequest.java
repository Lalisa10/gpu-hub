package com.trucdnd.gpu_hub_backend.workload_volume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttachVolumeRequest(
        @NotNull UUID volumeId,
        @NotBlank String mountPath
) {
}
