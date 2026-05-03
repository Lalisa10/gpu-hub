package com.trucdnd.gpu_hub_backend.workload.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.trucdnd.gpu_hub_backend.workload.dto.VolumeMountSpec;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;

@Component
public class NotebookSpecBuilder {

    private static final String API_VERSION = "kubeflow.org/v1";
    private static final String KIND = "Notebook";
    public static final String WORKLOAD_ID_LABEL = "gpu-hub/workload-id";

    /**
     * Builds a Kubeflow Notebook CR.
     *
     * @param workload   persisted workload entity (resources / image)
     * @param k8sName    auto-generated K8s resource name (e.g. "alice-notebook-ab3xy")
     * @param namespace  target K8s namespace (from TeamCluster)
     * @param mounts     PVC mounts to inject as podSpec.volumes / container.volumeMounts
     */
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
        if (mounts != null) {
            for (int i = 0; i < mounts.size(); i++) {
                VolumeMountSpec m = mounts.get(i);
                String volName = "vol-" + i;
                podVolumes.add(Map.of(
                        "name", volName,
                        "persistentVolumeClaim", Map.of("claimName", m.pvcName())));
                containerMounts.add(Map.of("name", volName, "mountPath", m.mountPath()));
            }
        }
        if (!containerMounts.isEmpty()) container.put("volumeMounts", containerMounts);

        Map<String, String> crLabels = Map.of(WORKLOAD_ID_LABEL, workload.getId().toString());

        Map<String, String> podLabels = new LinkedHashMap<>();
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
