package com.trucdnd.gpu_hub_backend.kubernetes.factory;

import java.util.Optional;
import java.util.UUID;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.common.constants.Cluster.Status;
import com.trucdnd.gpu_hub_backend.kubernetes.config.KubernetesProperties;
import com.trucdnd.gpu_hub_backend.object_storage.service.ObjectStorageService;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioBackedKubernetesClientFactoryTest {

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    private MinioBackedKubernetesClientFactory factory;

    @BeforeEach
    void setUp() {
        KubernetesProperties kubernetesProperties = new KubernetesProperties();
        kubernetesProperties.setKubeconfigBucket("kubeconfig");
        factory = new MinioBackedKubernetesClientFactory(
                clusterRepository,
                objectStorageService,
                kubernetesProperties
        );
    }

    @Test
    void createClient_byClusterId_readsKubeconfigFromConfiguredBucket() {
        UUID clusterId = UUID.fromString("00000000-0000-0000-0000-000000000111");
        Cluster cluster = sampleCluster(clusterId);

        when(clusterRepository.findById(clusterId)).thenReturn(Optional.of(cluster));
        when(objectStorageService.getObjectAsString("kubeconfig", "k3s.yaml"))
                .thenReturn(sampleKubeconfigYaml());

        try (KubernetesClient client = factory.createClient(clusterId)) {
            assertTrue(client.getConfiguration().getMasterUrl().startsWith("https://127.0.0.1:6443"));
        }

        verify(clusterRepository).findById(clusterId);
        verify(objectStorageService).getObjectAsString("kubeconfig", "k3s.yaml");
    }

    @Test
    void createClient_throwsWhenClusterMissing() {
        UUID missingId = UUID.fromString("00000000-0000-0000-0000-000000000222");
        when(clusterRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> factory.createClient(missingId));
    }

    @Test
    void createClient_throwsWhenKubeconfigRefMissing() {
        Cluster cluster = sampleCluster(UUID.fromString("00000000-0000-0000-0000-000000000333"));
        cluster.setKubeconfigRef(" ");

        assertThrows(IllegalArgumentException.class, () -> factory.createClient(cluster));
    }

    private Cluster sampleCluster(UUID clusterId) {
        Cluster cluster = Cluster.builder()
                .name("k3s-local")
                .description("Local k3s cluster")
                .apiEndpoint("https://127.0.0.1:6443")
                .kubeconfigRef("k3s.yaml")
                .status(Status.ACTIVE)
                .build();
        cluster.setId(clusterId);
        return cluster;
    }

    private String sampleKubeconfigYaml() {
        return """
                apiVersion: v1
                kind: Config
                clusters:
                  - name: k3s
                    cluster:
                      server: https://127.0.0.1:6443
                users:
                  - name: admin
                    user:
                      token: test-token
                contexts:
                  - name: k3s
                    context:
                      cluster: k3s
                      user: admin
                current-context: k3s
                """;
    }
}
