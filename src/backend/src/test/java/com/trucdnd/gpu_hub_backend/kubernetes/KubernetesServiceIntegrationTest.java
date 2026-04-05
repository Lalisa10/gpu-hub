package com.trucdnd.gpu_hub_backend.kubernetes;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.kubernetes.config.KubernetesProperties;
import com.trucdnd.gpu_hub_backend.kubernetes.factory.KubernetesClientFactory;
import com.trucdnd.gpu_hub_backend.kubernetes.factory.MinioBackedKubernetesClientFactory;
import com.trucdnd.gpu_hub_backend.kubernetes.service.KubernetesService;
import com.trucdnd.gpu_hub_backend.object_storage.service.ObjectStorageService;
import com.trucdnd.gpu_hub_backend.policy.entity.Policy;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
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

    // ── Queue (KAI Scheduler) ─────────────────────────────────────────────────

    @Test
    void createUpdateDeleteQueue() {
        String queueName = "test-gpu-hub-queue";
        Policy policy = Policy.builder()
                .name("test-policy")
                .maxPriority(100)
                .build();
        // Override gpuQuota to a small value via reflection-free builder
        // (set directly since Policy has setters via @Setter)
        policy.setGpuQuota(java.math.BigDecimal.valueOf(2));
        policy.setGpuLimit(java.math.BigDecimal.valueOf(4));
        policy.setOverQuotaWeight(java.math.BigDecimal.ONE);

        try {
            // Create
            GenericKubernetesResource created = kubernetesService.createProjectQueue(
                    cluster, queueName, null, policy);
            log.info("Created queue: {}", created.getMetadata().getName());
            assertEquals(queueName, created.getMetadata().getName());

            // Update — change quota
            policy.setGpuQuota(java.math.BigDecimal.valueOf(3));
            GenericKubernetesResource updated = kubernetesService.updateProjectQueue(
                    cluster, queueName, policy);
            log.info("Updated queue: {}", updated.getMetadata().getName());

            @SuppressWarnings("unchecked")
            Map<String, Object> spec = (Map<String, Object>) updated.getAdditionalProperties().get("spec");
            log.info("  deservedGPUs: {}", spec.get("deservedGPUs"));

        } finally {
            kubernetesService.deleteProjectQueue(cluster, queueName);
            log.info("Deleted queue: {}", queueName);
        }
    }

    // ── Deployment ────────────────────────────────────────────────────────────

    @Test
    void createUpdateDeleteDeployment() {
        String namespace = "default";
        String deploymentName = "test-gpu-hub-deploy";

        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                    .withName(deploymentName)
                    .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                    .withReplicas(1)
                    .withNewSelector()
                        .addToMatchLabels("app", deploymentName)
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .addToLabels("app", deploymentName)
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName("nginx")
                                .withImage("nginx:alpine")
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();

        try {
            Deployment created = kubernetesService.createDeployment(cluster, namespace, deployment);
            log.info("Created deployment: {}", created.getMetadata().getName());
            assertEquals(deploymentName, created.getMetadata().getName());

            // Scale up to 2
            deployment.getSpec().setReplicas(2);
            Deployment updated = kubernetesService.updateDeployment(cluster, namespace, deployment);
            log.info("Updated deployment replicas: {}", updated.getSpec().getReplicas());
            assertEquals(2, updated.getSpec().getReplicas());

        } finally {
            kubernetesService.deleteDeployment(cluster, namespace, deploymentName);
            log.info("Deleted deployment: {}", deploymentName);
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

    // ── Notebook CRD (Kubeflow) ───────────────────────────────────────────────

    @Test
    void createAndDeleteNotebook() {
        String namespace = "default";
        String notebookName = "test-gpu-hub-notebook";

        GenericKubernetesResource notebook = new GenericKubernetesResourceBuilder()
                .withApiVersion("kubeflow.org/v1")
                .withKind("Notebook")
                .withNewMetadata()
                    .withName(notebookName)
                    .withNamespace(namespace)
                .endMetadata()
                .withAdditionalProperties(Map.of(
                        "spec", Map.of(
                                "template", Map.of(
                                        "spec", Map.of(
                                                "containers", List.of(Map.of(
                                                        "name", "notebook",
                                                        "image", "kubeflownotebookswg/jupyter-scipy:v1.7.0",
                                                        "resources", Map.of(
                                                                "requests", Map.of(
                                                                        "cpu", "0.5",
                                                                        "memory", "1Gi")))))))))
                .build();

        try {
            GenericKubernetesResource created = kubernetesService.createNotebook(cluster, notebook);
            log.info("Created notebook: {}", created.getMetadata().getName());
            assertEquals(notebookName, created.getMetadata().getName());
        } finally {
            kubernetesService.deleteNotebook(cluster, namespace, notebookName);
            log.info("Deleted notebook: {}", notebookName);
        }
    }
}
