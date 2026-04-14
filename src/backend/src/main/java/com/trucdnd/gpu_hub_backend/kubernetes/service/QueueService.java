package com.trucdnd.gpu_hub_backend.kubernetes.service;

import org.springframework.stereotype.Service;

import com.trucdnd.gpu_hub_backend.kubernetes.factory.KubernetesClientFactory;

import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;

@Service
public class QueueService extends CustomResourceService {

    private static final ResourceDefinitionContext QUEUE_CTX = new ResourceDefinitionContext.Builder()
            .withGroup("scheduling.run.ai")
            .withVersion("v2")
            .withKind("Queue")
            .withNamespaced(false)
            .withPlural("queues")
            .build();

    public QueueService(KubernetesClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    protected ResourceDefinitionContext context() {
        return QUEUE_CTX;
    }

    @Override
    public boolean isClusterScoped() {
        return true;
    }
}
