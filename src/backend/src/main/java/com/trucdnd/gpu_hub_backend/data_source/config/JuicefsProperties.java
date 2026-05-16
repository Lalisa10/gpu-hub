package com.trucdnd.gpu_hub_backend.data_source.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "juicefs")
@Getter
@Setter
public class JuicefsProperties {

    private String storageClass = "juicefs-sc";
    private String pvcSize = "10Gi";
    private String migrationImage = "minio/mc:latest";
    private int backoffLimit = 3;
}
