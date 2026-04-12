package com.trucdnd.gpu_hub_backend.kubernetes.service;

import org.springframework.stereotype.Service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.kubernetes.factory.KubernetesClientFactory;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotebookService {

    private static final ResourceDefinitionContext NOTEBOOK_CTX = new ResourceDefinitionContext.Builder()
            .withGroup("kubeflow.org")
            .withVersion("v1")
            .withKind("Notebook")
            .withNamespaced(true)
            .withPlural("notebooks")
            .build();

    private final KubernetesClientFactory clientFactory;

    public GenericKubernetesResource createNotebook(Cluster cluster, GenericKubernetesResource notebook) {
        KubernetesClient client = clientFactory.createClient(cluster);
        String namespace = notebook.getMetadata().getNamespace();
        return client.genericKubernetesResources(NOTEBOOK_CTX)
                .inNamespace(namespace)
                .resource(notebook)
                .create();
    }

    public void deleteNotebook(Cluster cluster, String namespace, String notebookName) {
        KubernetesClient client = clientFactory.createClient(cluster);
        client.genericKubernetesResources(NOTEBOOK_CTX)
                .inNamespace(namespace)
                .withName(notebookName)
                .delete();
    }
}
