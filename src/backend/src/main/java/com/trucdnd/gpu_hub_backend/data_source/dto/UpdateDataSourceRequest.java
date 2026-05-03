package com.trucdnd.gpu_hub_backend.data_source.dto;

import com.trucdnd.gpu_hub_backend.common.constants.DataSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateDataSourceRequest(
        @NotNull DataSource.Status status,
        @NotBlank String bucketUrl,
        @NotBlank String accessKey,
        @NotBlank String secretKey
) {
}
