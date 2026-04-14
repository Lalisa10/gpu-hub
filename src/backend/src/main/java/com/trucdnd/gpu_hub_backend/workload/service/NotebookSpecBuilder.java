package com.trucdnd.gpu_hub_backend.workload.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.trucdnd.gpu_hub_backend.workload.entity.Workload;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;

@Component
public class NotebookSpecBuilder {

    private static final String API_VERSION = "kubeflow.org/v1";
    private static final String KIND = "Notebook";

    public GenericKubernetesResource build(Workload workload, String namespace) {
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
        if (workload.getRequestedCpuLimit() != null) {
            limits.put("cpu", workload.getRequestedCpuLimit().toPlainString());
        }
        if (workload.getRequestedMemoryLimit() != null) {
            limits.put("memory", workload.getRequestedMemoryLimit() + "Mi");
        }
        if (!requests.isEmpty()) resources.put("requests", requests);
        if (!limits.isEmpty()) resources.put("limits", limits);

        Map<String, Object> container = new HashMap<>();
        container.put("name", workload.getK8sResourceName());
        container.put("image", "kubeflownotebookswg/jupyter-scipy:latest");
        if (!resources.isEmpty()) container.put("resources", resources);

        Map<String, Object> podSpec = Map.of("containers", List.of(container));
        Map<String, Object> template = Map.of("spec", podSpec);
        Map<String, Object> spec = Map.of("template", template);

        return new GenericKubernetesResourceBuilder()
                .withApiVersion(API_VERSION)
                .withKind(KIND)
                .withNewMetadata()
                    .withName(workload.getK8sResourceName())
                    .withNamespace(namespace)
                .endMetadata()
                .withAdditionalProperties(Map.of("spec", spec))
                .build();
    }
}
