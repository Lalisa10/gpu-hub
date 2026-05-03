package com.trucdnd.gpu_hub_backend.data_source.dto;

import com.trucdnd.gpu_hub_backend.common.constants.DataSource;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.Optional;

@Builder
public record PatchDataSourceRequest(
        Optional<DataSource.Status> status,
        Optional<@NotBlank String> bucketUrl,
        Optional<@NotBlank String> accessKey,
        Optional<@NotBlank String> secretKey
) {
}
