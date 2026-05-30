package com.trucdnd.gpu_hub_backend.kubernetes.factory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.kubernetes.config.KubernetesProperties;
import com.trucdnd.gpu_hub_backend.object_storage.service.ObjectStorageService;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MinioBackedKubernetesClientFactory implements KubernetesClientFactory {

    private final ClusterRepository clusterRepository;
    private final ObjectStorageService objectStorageService;
    private final KubernetesProperties kubernetesProperties;

    private final ConcurrentMap<UUID, KubernetesClient> clientCache = new ConcurrentHashMap<>();

    @Override
    public KubernetesClient createClient(UUID clusterId) {
        return clientCache.computeIfAbsent(clusterId, id -> {
            Cluster cluster = clusterRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Cluster not found with id: " + id));
            return buildClient(loadKubeconfig(cluster));
        });
    }

    @Override
    public KubernetesClient createClient(Cluster cluster) {
        return clientCache.computeIfAbsent(cluster.getId(), id -> buildClient(loadKubeconfig(cluster)));
    }

    @Override
    public KubernetesClient createClientFromKubeconfig(String kubeconfig) {
        return buildClient(kubeconfig);
    }

    @Override
    public void evict(UUID clusterId) {
        KubernetesClient client = clientCache.remove(clusterId);
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.debug("Error closing cached openclient for cluster {}: {}", clusterId, e.getMessage());
            }
        }
    }

    @PreDestroy
    public void closeAll() {
        clientCache.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception ignored) {
            }
        });
        clientCache.clear();
    }

    private String loadKubeconfig(Cluster cluster) {
        String kubeconfigRef = cluster.getKubeconfigRef();
        if (kubeconfigRef == null || kubeconfigRef.isBlank()) {
            throw new IllegalArgumentException("Cluster " + cluster.getId() + " does not have kubeconfigRef");
        }
        return objectStorageService.getObjectAsString(
                kubernetesProperties.getKubeconfigBucket(),
                kubeconfigRef
        );
    }

    private KubernetesClient buildClient(String kubeconfig) {
        Config config = Config.fromKubeconfig(kubeconfig);
        config.setConnectionTimeout(kubernetesProperties.getConnectionTimeoutMs());
        config.setRequestTimeout(kubernetesProperties.getRequestTimeoutMs());
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
