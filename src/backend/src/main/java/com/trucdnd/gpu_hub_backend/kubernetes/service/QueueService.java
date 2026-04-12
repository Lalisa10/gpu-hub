package com.trucdnd.gpu_hub_backend.kubernetes.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.kubernetes.factory.KubernetesClientFactory;
import com.trucdnd.gpu_hub_backend.policy.entity.Policy;
import com.trucdnd.gpu_hub_backend.project.entity.Project;
import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueService {

    private static final ResourceDefinitionContext QUEUE_CTX = new ResourceDefinitionContext.Builder()
            .withGroup("scheduling.run.ai")
            .withVersion("v2")
            .withKind("Queue")
            .withNamespaced(false)
            .withPlural("queues")
            .build();

    private final KubernetesClientFactory clientFactory;

    public GenericKubernetesResource createTeamQueue(TeamCluster teamCluster) {
        String queueName = buildTeamQueueName(teamCluster);
        return createQueue(teamCluster.getCluster(), queueName, null, teamCluster.getPolicy());
    }

    public GenericKubernetesResource createProjectQueue(Project project, String parentQueueName) {
        return createQueue(project.getCluster(), project.getName(), parentQueueName, project.getPolicy());
    }

    public GenericKubernetesResource updateTeamQueue(TeamCluster teamCluster) {
        String queueName = buildTeamQueueName(teamCluster);
        return updateQueue(teamCluster.getCluster(), queueName, teamCluster.getPolicy());
    }

    public GenericKubernetesResource updateProjectQueue(Project project) {
        return updateQueue(project.getCluster(), project.getName(), project.getPolicy());
    }

    public void deleteTeamQueue(TeamCluster teamCluster) {
        deleteQueue(teamCluster.getCluster(), buildTeamQueueName(teamCluster));
    }

    public void deleteProjectQueue(Project project) {
        deleteQueue(project.getCluster(), project.getName());
    }

    public String getTeamQueueName(TeamCluster teamCluster) {
        return buildTeamQueueName(teamCluster);
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

    private Map<String, Object> buildQueueSpec(String parentQueueName, Policy policy) {
        Map<String, Object> spec = new HashMap<>();

        if (parentQueueName != null && !parentQueueName.isBlank()) {
            spec.put("parentQueue", parentQueueName);
        }

        int gpuOverQuotaWeight = (policy != null && policy.getGpuOverQuotaWeight() != null)
                ? policy.getGpuOverQuotaWeight() : 1;
        int cpuOverQuotaWeight = (policy != null && policy.getCpuOverQuotaWeight() != null)
                ? policy.getCpuOverQuotaWeight() : 1;
        int memoryOverQuotaWeight = (policy != null && policy.getMemoryOverQuotaWeight() != null)
                ? policy.getMemoryOverQuotaWeight() : 1;

        Map<String, Object> gpuResource = new HashMap<>();
        gpuResource.put("quota", policy != null && policy.getGpuQuota() != null
                ? policy.getGpuQuota().doubleValue() : 0);
        gpuResource.put("limit", policy != null && policy.getGpuLimit() != null
                ? policy.getGpuLimit().doubleValue() : -1);
        gpuResource.put("overQuotaWeight", gpuOverQuotaWeight);

        Map<String, Object> cpuResource = new HashMap<>();
        cpuResource.put("quota", policy != null && policy.getCpuQuota() != null
                ? policy.getCpuQuota().doubleValue() : 0);
        cpuResource.put("limit", policy != null && policy.getCpuLimit() != null
                ? policy.getCpuLimit().doubleValue() : -1);
        cpuResource.put("overQuotaWeight", cpuOverQuotaWeight);

        Map<String, Object> memoryResource = new HashMap<>();
        memoryResource.put("quota", policy != null && policy.getMemoryQuota() != null
                ? policy.getMemoryQuota() : 0);
        memoryResource.put("limit", policy != null && policy.getMemoryLimit() != null
                ? policy.getMemoryLimit() : -1);
        memoryResource.put("overQuotaWeight", memoryOverQuotaWeight);

        spec.put("resources", Map.of(
                "gpu", gpuResource,
                "cpu", cpuResource,
                "memory", memoryResource));

        return spec;
    }
}
