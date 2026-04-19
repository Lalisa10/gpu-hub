package com.trucdnd.gpu_hub_backend.workload.dto;

import java.time.OffsetDateTime;

public record PodInfoDto(
        String name,
        String ip,
        String nodeName,
        String phase,
        String status,
        boolean ready,
        int restartCount,
        OffsetDateTime startTime
) {
}
