package com.trucdnd.gpu_hub_backend.data_source.dto;

import com.trucdnd.gpu_hub_backend.common.constants.DataSource;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DataSourceDto(
        UUID id,
        UUID clusterId,
        UUID teamId,
        UUID createdById,
        UUID volumeId,
        String pvcName,
        DataSource.Status status,
        String bucketUrl,
        String accessKey,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
