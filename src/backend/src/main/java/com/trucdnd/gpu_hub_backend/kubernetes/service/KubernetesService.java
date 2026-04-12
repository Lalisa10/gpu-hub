package com.trucdnd.gpu_hub_backend.kubernetes.service;

import java.io.OutputStream;
import java.util.List;

import org.springframework.stereotype.Service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.kubernetes.factory.KubernetesClientFactory;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KubernetesService {

    private final KubernetesClientFactory clientFactory;

    // ── Namespace ─────────────────────────────────────────────────────────────

    public Namespace createNamespace(Cluster cluster, String nsName) {
        KubernetesClient client = clientFactory.createClient(cluster);
        return client.namespaces().resource(
                new NamespaceBuilder()
                        .withNewMetadata().withName(nsName).endMetadata()
                        .build())
                .create();
    }

    public void deleteNamespace(Cluster cluster, String nsName) {
        KubernetesClient client = clientFactory.createClient(cluster);
        client.namespaces().withName(nsName).delete();
    }

    public List<Namespace> getNamespaces(Cluster cluster) {
        KubernetesClient client = clientFactory.createClient(cluster);
        return client.namespaces().list().getItems();
    }

    // ── Deployment ────────────────────────────────────────────────────────────

    public Deployment createDeployment(Cluster cluster, String namespace, Deployment deployment) {
        KubernetesClient client = clientFactory.createClient(cluster);
        return client.apps().deployments().inNamespace(namespace).resource(deployment).create();
    }

    public Deployment updateDeployment(Cluster cluster, String namespace, Deployment deployment) {
        KubernetesClient client = clientFactory.createClient(cluster);
        return client.apps().deployments().inNamespace(namespace).resource(deployment).update();
    }

    public void deleteDeployment(Cluster cluster, String namespace, String deploymentName) {
        KubernetesClient client = clientFactory.createClient(cluster);
        client.apps().deployments().inNamespace(namespace).withName(deploymentName).delete();
    }

    // ── Pod logs ──────────────────────────────────────────────────────────────

    public String getPodLogs(Cluster cluster, String namespace, String podName) {
        KubernetesClient client = clientFactory.createClient(cluster);
        return client.pods().inNamespace(namespace).withName(podName).getLog();
    }

    public String getPodLogs(Cluster cluster, String namespace, String podName, String containerName) {
        KubernetesClient client = clientFactory.createClient(cluster);
        return client.pods().inNamespace(namespace).withName(podName)
                .inContainer(containerName).getLog();
    }

    public LogWatch watchPodLogs(Cluster cluster, String namespace, String podName, OutputStream out) {
        KubernetesClient client = clientFactory.createClient(cluster);
        return client.pods().inNamespace(namespace).withName(podName).watchLog(out);
    }

    public LogWatch watchPodLogs(Cluster cluster, String namespace, String podName,
            String containerName, OutputStream out) {
        KubernetesClient client = clientFactory.createClient(cluster);
        return client.pods().inNamespace(namespace).withName(podName)
                .inContainer(containerName).watchLog(out);
    }
}
