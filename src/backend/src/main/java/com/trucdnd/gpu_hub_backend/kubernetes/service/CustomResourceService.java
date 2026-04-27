package com.trucdnd.gpu_hub_backend.kubernetes.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.common.exception.KubernetesOperationException;
import com.trucdnd.gpu_hub_backend.kubernetes.factory.KubernetesClientFactory;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;

public abstract class CustomResourceService {

    protected final KubernetesClientFactory clientFactory;

    protected CustomResourceService(KubernetesClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    protected abstract ResourceDefinitionContext context();

    public abstract boolean isClusterScoped();

    public GenericKubernetesResource create(Cluster cluster, GenericKubernetesResource resource) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            if (isClusterScoped()) {
                return client.genericKubernetesResources(context())
                        .resource(resource)
                        .create();
            }
            String namespace = resource.getMetadata().getNamespace();
            return client.genericKubernetesResources(context())
                    .inNamespace(namespace)
                    .resource(resource)
                    .create();
        } catch (KubernetesClientException e) {
            throw k8sError("create", cluster, e);
        }
    }

    public GenericKubernetesResource update(Cluster cluster, GenericKubernetesResource resource) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            String name = resource.getMetadata().getName();
            PatchContext patch = PatchContext.of(PatchType.SERVER_SIDE_APPLY);
            if (isClusterScoped()) {
                return client.genericKubernetesResources(context())
                        .withName(name)
                        .patch(patch, resource);
            }
            String namespace = resource.getMetadata().getNamespace();
            return client.genericKubernetesResources(context())
                    .inNamespace(namespace)
                    .withName(name)
                    .patch(patch, resource);
        } catch (KubernetesClientException e) {
            throw k8sError("update", cluster, e);
        }
    }

    public GenericKubernetesResource get(Cluster cluster, String namespace, String name) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            if (isClusterScoped()) {
                return client.genericKubernetesResources(context())
                        .withName(name)
                        .get();
            }
            return client.genericKubernetesResources(context())
                    .inNamespace(namespace)
                    .withName(name)
                    .get();
        } catch (KubernetesClientException e) {
            throw k8sError("get", cluster, e);
        }
    }

    public void delete(Cluster cluster, String namespace, String name) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            if (isClusterScoped()) {
                client.genericKubernetesResources(context())
                        .withName(name)
                        .delete();
                return;
            }
            client.genericKubernetesResources(context())
                    .inNamespace(namespace)
                    .withName(name)
                    .delete();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) return;
            throw k8sError("delete", cluster, e);
        }
    }

    public void deleteByLabel(Cluster cluster, String namespace, String labelKey, String labelValue) {
        try {
            KubernetesClient client = clientFactory.createClient(cluster);
            if (isClusterScoped()) {
                client.genericKubernetesResources(context())
                        .withLabel(labelKey, labelValue)
                        .delete();
                return;
            }
            client.genericKubernetesResources(context())
                    .inNamespace(namespace)
                    .withLabel(labelKey, labelValue)
                    .delete();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) return;
            throw k8sError("deleteByLabel " + labelKey + "=" + labelValue, cluster, e);
        }
    }

    protected KubernetesOperationException k8sError(String action, Cluster cluster, KubernetesClientException e) {
        String clusterName = cluster != null ? cluster.getName() : "unknown";
        String kind = context() != null ? context().getKind() : "resource";
        String detail = e.getMessage() != null ? e.getMessage() : "Unknown Kubernetes error";
        return new KubernetesOperationException(
                "Kubernetes operation failed while trying to " + action + " " + kind
                        + " on cluster '" + clusterName + "': " + detail,
                e
        );
    }
}
