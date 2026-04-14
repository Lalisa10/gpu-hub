package com.trucdnd.gpu_hub_backend.policy.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.trucdnd.gpu_hub_backend.policy.entity.Policy;
import com.trucdnd.gpu_hub_backend.project.entity.Project;
import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;

@Component
public class QueueSpecBuilder {

    private static final String API_VERSION = "scheduling.run.ai/v2";
    private static final String KIND = "Queue";

    public String buildTeamQueueName(TeamCluster teamCluster) {
        return sanitizeQueueName("team-" + teamCluster.getTeam().getName());
    }

    public String buildProjectQueueName(Project project) {
        return project.getName();
    }

    public GenericKubernetesResource buildTeamQueue(TeamCluster teamCluster) {
        return build(buildTeamQueueName(teamCluster), null, teamCluster.getPolicy());
    }

    public GenericKubernetesResource buildProjectQueue(Project project, String parentQueueName) {
        return build(buildProjectQueueName(project), parentQueueName, project.getPolicy());
    }

    private GenericKubernetesResource build(String queueName, String parentQueueName, Policy policy) {
        Map<String, Object> spec = buildSpec(parentQueueName, policy);
        return new GenericKubernetesResourceBuilder()
                .withApiVersion(API_VERSION)
                .withKind(KIND)
                .withNewMetadata().withName(queueName).endMetadata()
                .withAdditionalProperties(Map.of("spec", spec))
                .build();
    }

    private Map<String, Object> buildSpec(String parentQueueName, Policy policy) {
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

    private String sanitizeQueueName(String rawName) {
        String normalized = rawName == null ? "" : rawName.trim().toLowerCase()
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Invalid queue name derived from team name");
        }
        if (normalized.length() > 63) {
            normalized = normalized.substring(0, 63).replaceAll("-+$", "");
        }
        return normalized;
    }
}
