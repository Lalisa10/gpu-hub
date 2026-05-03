package com.trucdnd.gpu_hub_backend.data_volume.dto;

import com.trucdnd.gpu_hub_backend.common.constants.DataVolume;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDataVolumeRequest(
        @NotNull UUID teamId,
        @NotNull UUID clusterId,
        @NotNull UUID createdById,
        @NotBlank String pvcName,
        @NotNull DataVolume.VolumeType volumeType
) {
}
