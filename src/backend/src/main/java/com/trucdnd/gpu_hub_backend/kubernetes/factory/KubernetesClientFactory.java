package com.trucdnd.gpu_hub_backend.kubernetes.factory;

import java.util.UUID;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;

import io.fabric8.kubernetes.client.KubernetesClient;

public interface KubernetesClientFactory {

    KubernetesClient createClient(UUID clusterId);

    KubernetesClient createClient(Cluster cluster);

    /** Build a client straight from a raw kubeconfig string (e.g. to validate an upload). */
    KubernetesClient createClientFromKubeconfig(String kubeconfig);
}
// 