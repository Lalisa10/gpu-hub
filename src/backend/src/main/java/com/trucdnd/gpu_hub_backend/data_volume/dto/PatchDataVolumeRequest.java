package com.trucdnd.gpu_hub_backend.data_volume.dto;

import com.trucdnd.gpu_hub_backend.common.constants.DataVolume;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.Optional;

@Builder
public record PatchDataVolumeRequest(
        Optional<@NotBlank String> pvcName,
        Optional<DataVolume.VolumeType> volumeType
) {
}
