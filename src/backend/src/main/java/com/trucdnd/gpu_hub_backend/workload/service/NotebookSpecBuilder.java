package com.trucdnd.gpu_hub_backend.workload.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trucdnd.gpu_hub_backend.kubernetes.util.K8sLabels;
import com.trucdnd.gpu_hub_backend.workload.dto.VolumeMountSpec;
import com.trucdnd.gpu_hub_backend.workload.dto.WorkloadExtra;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;

@Component
public class NotebookSpecBuilder {

    private static final String API_VERSION = "kubeflow.org/v1";
    private static final String KIND = "Notebook";
    public static final String WORKLOAD_ID_LABEL = K8sLabels.WORKLOAD_ID;
    public static final String TEAM_DATA_VOLUME_NAME = "team-data";
    private static final String SHM_VOLUME_NAME = "dshm";
    private static final String SHM_MOUNT_PATH = "/dev/shm";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GenericKubernetesResource build(Workload workload, String k8sName, String namespace,
            String queueName, List<VolumeMountSpec> mounts) {
        Map<String, Object> resources = new HashMap<>();
        Map<String, String> requests = new HashMap<>();
        Map<String, String> limits = new HashMap<>();

        if (workload.getRequestedCpu() != null) {
            requests.put("cpu", workload.getRequestedCpu().toPlainString());
        }
        if (workload.getRequestedMemory() != null) {
            requests.put("memory", workload.getRequestedMemory() + "Mi");
        }
        if (workload.getRequestedGpu() != null) {
            requests.put("nvidia.com/gpu", workload.getRequestedGpu().toPlainString());
            limits.put("nvidia.com/gpu", workload.getRequestedGpu().toPlainString());
        }

        if (!requests.isEmpty()) resources.put("requests", requests);
        if (!limits.isEmpty()) resources.put("limits", limits);

        Map<String, Object> container = new HashMap<>();
        container.put("name", k8sName);
        container.put("image", workload.getImage());
        if (!resources.isEmpty()) container.put("resources", resources);

        List<Map<String, Object>> podVolumes = new ArrayList<>();
        List<Map<String, Object>> containerMounts = new ArrayList<>();
        if (mounts != null && !mounts.isEmpty()) {
            String teamPvcName = mounts.get(0).pvcName();
            podVolumes.add(Map.of(
                    "name", TEAM_DATA_VOLUME_NAME,
                    "persistentVolumeClaim", Map.of("claimName", teamPvcName)));
            for (VolumeMountSpec m : mounts) {
                Map<String, Object> mount = new LinkedHashMap<>();
                mount.put("name", TEAM_DATA_VOLUME_NAME);
                mount.put("mountPath", m.mountPath());
                mount.put("subPath", m.folderName());
                if (m.readOnly()) mount.put("readOnly", true);
                containerMounts.add(mount);
            }
        }

        // Shared-memory volume (/dev/shm) — opt-in via extra.shmSize
        WorkloadExtra workloadExtra = WorkloadExtra.parse(workload.getExtra(), objectMapper);
        if (workloadExtra.hasShmSize()) {
            podVolumes.add(Map.of(
                    "name", SHM_VOLUME_NAME,
                    "emptyDir", Map.of("medium", "Memory", "sizeLimit", workloadExtra.shmSize())));
            containerMounts.add(Map.of(
                    "name", SHM_VOLUME_NAME,
                    "mountPath", SHM_MOUNT_PATH));
        }

        if (!containerMounts.isEmpty()) container.put("volumeMounts", containerMounts);

        Map<String, String> baseLabels = K8sLabels.standard(
                K8sLabels.OWNER_WORKLOAD,
                workload.getProject().getTeam().getName(),
                workload.getCluster().getName(),
                workload.getSubmittedBy().getUsername());
        baseLabels.put(K8sLabels.PROJECT, K8sLabels.sanitize(workload.getProject().getName()));

        Map<String, String> crLabels = new LinkedHashMap<>(baseLabels);
        crLabels.put(WORKLOAD_ID_LABEL, workload.getId().toString());

        Map<String, String> podLabels = new LinkedHashMap<>(baseLabels);
        podLabels.put(WORKLOAD_ID_LABEL, workload.getId().toString());
        podLabels.put("kai.scheduler/queue", queueName);

        Map<String, Object> podSpec = new HashMap<>();
        podSpec.put("containers", List.of(container));
        podSpec.put("schedulerName", "kai-scheduler");
        podSpec.put("priorityClassName", workload.getPriorityClass().dbValue);
        if (!podVolumes.isEmpty()) podSpec.put("volumes", podVolumes);

        Map<String, Object> templateMetadata = Map.of("labels", podLabels);
        Map<String, Object> template = Map.of("metadata", templateMetadata, "spec", podSpec);
        Map<String, Object> spec = Map.of("template", template);

        return new GenericKubernetesResourceBuilder()
                .withApiVersion(API_VERSION)
                .withKind(KIND)
                .withNewMetadata()
                    .withName(k8sName)
                    .withNamespace(namespace)
                    .withLabels(crLabels)
                .endMetadata()
                .withAdditionalProperties(Map.of("spec", spec))
                .build();
    }
}
