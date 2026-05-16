package com.trucdnd.gpu_hub_backend.workload_source.dto;

import java.util.UUID;

public record WorkloadSourceDto(
        UUID workloadId,
        UUID sourceId,
        String sourceName,
        String mountPath
) {
}
