package com.trucdnd.gpu_hub_backend.data_source.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDataSourceRequest(
        @NotNull UUID clusterId,
        @NotNull UUID createdById,
        @NotNull UUID teamId,
        @NotBlank String pvcName,
        @NotBlank String bucketUrl,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        String sourcePath
) {
}
