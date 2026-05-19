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

    /** Fabric8 client connection timeout (ms) — bounds calls to unreachable clusters. */
    private int connectionTimeoutMs = 10_000;

    /** Fabric8 client request timeout (ms) — bounds slow/hanging API responses. */
    private int requestTimeoutMs = 15_000;
}
