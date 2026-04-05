package com.trucdnd.gpu_hub_backend.object_storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "object-storage.minio")
@Getter
@Setter
public class MinioProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
}
