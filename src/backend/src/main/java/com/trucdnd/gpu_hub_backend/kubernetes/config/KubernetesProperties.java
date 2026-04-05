package com.trucdnd.gpu_hub_backend.kubernetes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "kubernetes")
@Getter
@Setter
public class KubernetesProperties {

    private String kubeconfigBucket;
}
