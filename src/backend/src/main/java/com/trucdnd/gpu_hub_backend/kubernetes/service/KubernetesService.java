package com.trucdnd.gpu_hub_backend.kubernetes.service;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.kubernetes.factory.KubernetesClientFactory;
import com.trucdnd.gpu_hub_backend.policy.entity.Policy;
import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KubernetesService {

    private static final ResourceDefinitionContext QUEUE_CTX = new ResourceDefinitionContext.Builder()
            .withGroup("scheduling.run.ai")
            .withVersion("v2")
            .withKind("Queue")
            .withNamespaced(false)
            .withPlural("queues")
            .build();

    private static final ResourceDefinitionContext NOTEBOOK_CTX = new ResourceDefinitionContext.Builder()
            .withGroup("kubeflow.org")
            .withVersion("v1")
            .withKind("Notebook")
            .withNamespaced(true)
            .withPlural("notebooks")
            .build();

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

    // ── Queue (KAI Scheduler) ─────────────────────────────────────────────────
    // Queues represent resource policies for teams/projects internally.
    // Hierarchy: team-level queue (from TeamCluster) → project-level queue (child).

    /**
     * Creates a cluster-scoped KAI Queue from a TeamCluster assignment.
     * The queue name is derived from the team + cluster combination.
     */
    public GenericKubernetesResource createTeamQueue(Cluster cluster, TeamCluster teamCluster) {
        String queueName = buildTeamQueueName(teamCluster);
        return createQueue(cluster, queueName, null, teamCluster.getPolicy());
    }

    /**
     * Creates a project-scoped KAI Queue as a child of the team queue.
     * The queue name is derived from the project namespace.
     */
    public GenericKubernetesResource createProjectQueue(Cluster cluster, String projectNamespace,
            String parentQueueName, Policy policy) {
        return createQueue(cluster, projectNamespace, parentQueueName, policy);
    }

    public GenericKubernetesResource updateTeamQueue(Cluster cluster, TeamCluster teamCluster) {
        String queueName = buildTeamQueueName(teamCluster);
        return updateQueue(cluster, queueName, teamCluster.getPolicy());
    }

    public GenericKubernetesResource updateProjectQueue(Cluster cluster, String projectNamespace, Policy policy) {
        return updateQueue(cluster, projectNamespace, policy);
    }

    public void deleteTeamQueue(Cluster cluster, TeamCluster teamCluster) {
        deleteQueue(cluster, buildTeamQueueName(teamCluster));
    }

    public void deleteProjectQueue(Cluster cluster, String projectNamespace) {
        deleteQueue(cluster, projectNamespace);
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

    /**
     * Streams pod logs to the given OutputStream. The caller is responsible for
     * closing the returned LogWatch.
     */
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

    // ── Notebook CRD (Kubeflow) ───────────────────────────────────────────────

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

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildTeamQueueName(TeamCluster teamCluster) {
        return ("team-" + teamCluster.getTeam().getName()).toLowerCase().replaceAll("[^a-z0-9-]", "-");
    }

    private GenericKubernetesResource createQueue(Cluster cluster, String queueName,
            String parentQueueName, Policy policy) {
        KubernetesClient client = clientFactory.createClient(cluster);

        Map<String, Object> spec = buildQueueSpec(parentQueueName, policy);

        GenericKubernetesResource queue = new GenericKubernetesResourceBuilder()
                .withApiVersion("scheduling.run.ai/v2")
                .withKind("Queue")
                .withNewMetadata().withName(queueName).endMetadata()
                .withAdditionalProperties(Map.of("spec", spec))
                .build();

        return client.genericKubernetesResources(QUEUE_CTX).resource(queue).create();
    }

    private GenericKubernetesResource updateQueue(Cluster cluster, String queueName, Policy policy) {
        KubernetesClient client = clientFactory.createClient(cluster);

        Resource<GenericKubernetesResource> resource = client.genericKubernetesResources(QUEUE_CTX)
                .withName(queueName);

        GenericKubernetesResource existing = resource.get();
        if (existing == null) {
            throw new IllegalStateException("Queue not found: " + queueName);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> currentSpec = (Map<String, Object>) existing.getAdditionalProperties()
                .getOrDefault("spec", new HashMap<>());

        // Preserve parentQueue from existing spec; only update policy-driven fields
        String parentQueue = currentSpec.containsKey("parentQueue")
                ? (String) currentSpec.get("parentQueue")
                : null;

        Map<String, Object> newSpec = buildQueueSpec(parentQueue, policy);
        existing.setAdditionalProperties(Map.of("spec", newSpec));

        return client.genericKubernetesResources(QUEUE_CTX).resource(existing).update();
    }

    private void deleteQueue(Cluster cluster, String queueName) {
        KubernetesClient client = clientFactory.createClient(cluster);
        client.genericKubernetesResources(QUEUE_CTX).withName(queueName).delete();
    }

    /**
     * Builds the KAI Scheduler v2 Queue spec from a Policy.
     * Format: spec.resources.{gpu|cpu|memory}.{quota, limit, overQuotaWeight}
     * -1 means unlimited for limit fields.
     */
    private Map<String, Object> buildQueueSpec(String parentQueueName, Policy policy) {
        Map<String, Object> spec = new HashMap<>();

        if (parentQueueName != null && !parentQueueName.isBlank()) {
            spec.put("parentQueue", parentQueueName);
        }

        double overQuotaWeight = (policy != null && policy.getOverQuotaWeight() != null)
                ? policy.getOverQuotaWeight().doubleValue() : 1.0;

        Map<String, Object> gpuResource = new HashMap<>();
        gpuResource.put("quota", policy != null && policy.getGpuQuota() != null
                ? policy.getGpuQuota().doubleValue() : 0);
        gpuResource.put("limit", policy != null && policy.getGpuLimit() != null
                ? policy.getGpuLimit().doubleValue() : -1);
        gpuResource.put("overQuotaWeight", overQuotaWeight);

        Map<String, Object> cpuResource = new HashMap<>();
        cpuResource.put("quota", policy != null && policy.getCpuQuota() != null
                ? policy.getCpuQuota().doubleValue() : 0);
        cpuResource.put("limit", policy != null && policy.getCpuLimit() != null
                ? policy.getCpuLimit().doubleValue() : -1);
        cpuResource.put("overQuotaWeight", overQuotaWeight);

        Map<String, Object> memoryResource = new HashMap<>();
        memoryResource.put("quota", policy != null && policy.getMemoryQuota() != null
                ? policy.getMemoryQuota() : 0);
        memoryResource.put("limit", policy != null && policy.getMemoryLimit() != null
                ? policy.getMemoryLimit() : -1);
        memoryResource.put("overQuotaWeight", overQuotaWeight);

        spec.put("resources", Map.of(
                "gpu", gpuResource,
                "cpu", cpuResource,
                "memory", memoryResource));

        return spec;
    }
}
