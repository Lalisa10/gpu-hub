package com.trucdnd.gpu_hub_backend.kubernetes;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.kubernetes.config.KubernetesProperties;
import com.trucdnd.gpu_hub_backend.kubernetes.factory.KubernetesClientFactory;
import com.trucdnd.gpu_hub_backend.kubernetes.factory.MinioBackedKubernetesClientFactory;
import com.trucdnd.gpu_hub_backend.kubernetes.service.KubernetesService;
import com.trucdnd.gpu_hub_backend.object_storage.service.ObjectStorageService;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class KubernetesServiceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(KubernetesServiceIntegrationTest.class);
    private static final UUID CLUSTER_ID = UUID.fromString("395ba24a-f7d3-418b-8001-03da71aacec1");

    @Autowired private ClusterRepository clusterRepository;
    @Autowired private ObjectStorageService objectStorageService;
    @Autowired private KubernetesClientFactory factory;
    @Autowired private KubernetesService kubernetesService;
    @Autowired private KubernetesProperties kubernetesProperties;

    private Cluster cluster;

    @BeforeEach
    void setUp() {
        kubernetesProperties.setKubeconfigBucket("kubeconfig");
        factory = new MinioBackedKubernetesClientFactory(
                clusterRepository, objectStorageService, kubernetesProperties);

        cluster = clusterRepository.findById(CLUSTER_ID)
                .orElseThrow(() -> new EntityNotFoundException("Cluster not found: " + CLUSTER_ID));
    }

    // ── Namespace ─────────────────────────────────────────────────────────────

    @Test
    void listSpace() {
        List<Namespace> nsList = kubernetesService.getNamespaces(cluster);
        log.info("Namespace count: {}", nsList.size());
        nsList.forEach(ns -> log.info("  ns: {}", ns.getMetadata().getName()));
        assertFalse(nsList.isEmpty());
    }

    @Test
    void createAndDeleteNamespace() {
        String nsName = "test-gpu-hub-ns";
        try {
            Namespace created = kubernetesService.createNamespace(cluster, nsName);
            log.info("Created namespace: {}", created.getMetadata().getName());
            assertEquals(nsName, created.getMetadata().getName());
        } finally {
            kubernetesService.deleteNamespace(cluster, nsName);
            log.info("Deleted namespace: {}", nsName);
        }
    }


    // ── Pod logs ──────────────────────────────────────────────────────────────

    @Test
    void getPodLogs() {
        // List pods to find one running in default namespace
        var pods = factory.createClient(cluster).pods().inNamespace("default").list().getItems();
        if (pods.isEmpty()) {
            log.info("No pods in default namespace — skipping log test");
            return;
        }
        String podName = pods.get(0).getMetadata().getName();
        log.info("Fetching logs for pod: {}", podName);
        String logs = kubernetesService.getPodLogs(cluster, "default", podName);
        log.info("Log length: {} chars", logs == null ? 0 : logs.length());
    }

    @Test
    void watchPodLogs() throws Exception {
        var pods = factory.createClient(cluster).pods().inNamespace("default").list().getItems();
        if (pods.isEmpty()) {
            log.info("No pods in default namespace — skipping watch log test");
            return;
        }
        String podName = pods.get(0).getMetadata().getName();
        log.info("Watching logs for pod: {}", podName);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (LogWatch watch = kubernetesService.watchPodLogs(cluster, "default", podName, buffer)) {
            Thread.sleep(1000); // collect 1s of output
        }
        log.info("Streamed {} bytes", buffer.size());
    }

}
