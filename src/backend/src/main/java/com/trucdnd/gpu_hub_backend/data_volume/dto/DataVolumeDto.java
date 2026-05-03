package com.trucdnd.gpu_hub_backend.data_volume.dto;

import com.trucdnd.gpu_hub_backend.common.constants.DataVolume;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DataVolumeDto(
        UUID id,
        UUID teamId,
        UUID clusterId,
        UUID createdById,
        String pvcName,
        DataVolume.VolumeType volumeType,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
