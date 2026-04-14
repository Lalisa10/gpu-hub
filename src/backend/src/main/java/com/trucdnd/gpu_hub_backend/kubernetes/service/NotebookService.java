package com.trucdnd.gpu_hub_backend.kubernetes.service;

import org.springframework.stereotype.Service;

import com.trucdnd.gpu_hub_backend.kubernetes.factory.KubernetesClientFactory;

import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;

@Service
public class NotebookService extends CustomResourceService {

    private static final ResourceDefinitionContext NOTEBOOK_CTX = new ResourceDefinitionContext.Builder()
            .withGroup("kubeflow.org")
            .withVersion("v1")
            .withKind("Notebook")
            .withNamespaced(true)
            .withPlural("notebooks")
            .build();

    public NotebookService(KubernetesClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    protected ResourceDefinitionContext context() {
        return NOTEBOOK_CTX;
    }

    @Override
    public boolean isClusterScoped() {
        return false;
    }
}
