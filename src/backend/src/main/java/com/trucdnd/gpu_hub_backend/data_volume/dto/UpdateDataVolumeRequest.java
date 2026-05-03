package com.trucdnd.gpu_hub_backend.data_volume.dto;

import com.trucdnd.gpu_hub_backend.common.constants.DataVolume;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateDataVolumeRequest(
        @NotBlank String pvcName,
        @NotNull DataVolume.VolumeType volumeType
) {
}
