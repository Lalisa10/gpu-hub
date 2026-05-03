package com.trucdnd.gpu_hub_backend.workload_volume.dto;

import java.util.UUID;

public record WorkloadVolumeDto(
        UUID workloadId,
        UUID volumeId,
        String mountPath
) {
}
