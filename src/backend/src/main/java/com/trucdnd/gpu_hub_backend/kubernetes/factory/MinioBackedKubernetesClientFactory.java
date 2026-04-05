package com.trucdnd.gpu_hub_backend.kubernetes.factory;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.kubernetes.config.KubernetesProperties;
import com.trucdnd.gpu_hub_backend.object_storage.service.ObjectStorageService;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MinioBackedKubernetesClientFactory implements KubernetesClientFactory {

    private final ClusterRepository clusterRepository;
    private final ObjectStorageService objectStorageService;
    private final KubernetesProperties kubernetesProperties;

    @Override
    public KubernetesClient createClient(UUID clusterId) {
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + clusterId));
        return createClient(cluster);
    }

    @Override
    public KubernetesClient createClient(Cluster cluster) {
        String kubeconfigRef = cluster.getKubeconfigRef();
        if (kubeconfigRef == null || kubeconfigRef.isBlank()) {
            throw new IllegalArgumentException("Cluster " + cluster.getId() + " does not have kubeconfigRef");
        }

        String kubeconfig = objectStorageService.getObjectAsString(
                kubernetesProperties.getKubeconfigBucket(),
                kubeconfigRef
        );

        Config config = Config.fromKubeconfig(kubeconfig);
        if (cluster.getApiEndpoint() != null && !cluster.getApiEndpoint().isBlank()) {
            config.setMasterUrl(cluster.getApiEndpoint());
        }
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
