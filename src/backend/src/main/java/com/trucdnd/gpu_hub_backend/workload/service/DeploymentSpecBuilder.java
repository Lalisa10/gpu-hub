package com.trucdnd.gpu_hub_backend.workload.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trucdnd.gpu_hub_backend.workload.dto.LlmInferenceExtra;
import com.trucdnd.gpu_hub_backend.workload.dto.VolumeMountSpec;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
@Component
public class DeploymentSpecBuilder {

    private static final int VLLM_CONTAINER_PORT = 8000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Deployment build(Workload workload, String k8sName, String namespace, String queueName,
            List<VolumeMountSpec> mounts) {
        LlmInferenceExtra extra = parseExtra(workload.getExtra());
        if (extra.modelSource() == null || extra.modelSource().isBlank()) {
            throw new IllegalArgumentException("modelSource is required for LLM_INFERENCE workloads");
        }

        List<String> args = new ArrayList<>();
        args.add("--model");
        args.add(extra.modelSource());
        args.addAll(tokenizeVllmParams(extra.vllmParams()));

        List<EnvVar> env = buildEnv(extra.envVars());
        ResourceRequirements resources = buildResources(workload);

        Map<String, String> selectorLabels = Map.of(
                NotebookSpecBuilder.WORKLOAD_ID_LABEL, workload.getId().toString());

        Map<String, String> podLabels = new HashMap<>();
        podLabels.put(NotebookSpecBuilder.WORKLOAD_ID_LABEL, workload.getId().toString());
        podLabels.put("kai.scheduler/queue", queueName);

        int replicas = (extra.replicaCount() == null || extra.replicaCount() < 1)
                ? 1
                : extra.replicaCount();

        List<Volume> podVolumes = new ArrayList<>();
        List<VolumeMount> containerMounts = new ArrayList<>();
        if (mounts != null) {
            for (int i = 0; i < mounts.size(); i++) {
                VolumeMountSpec m = mounts.get(i);
                String volName = "vol-" + i;
                podVolumes.add(new VolumeBuilder()
                        .withName(volName)
                        .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder()
                                .withClaimName(m.pvcName())
                                .build())
                        .build());
                containerMounts.add(new VolumeMountBuilder()
                        .withName(volName)
                        .withMountPath(m.mountPath())
                        .build());
            }
        }

        return new DeploymentBuilder()
                .withNewMetadata()
                    .withName(k8sName)
                    .withNamespace(namespace)
                    .withLabels(selectorLabels)
                .endMetadata()
                .withNewSpec()
                    .withReplicas(replicas)
                    .withNewSelector()
                        .withMatchLabels(selectorLabels)
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .withLabels(podLabels)
                        .endMetadata()
                        .withNewSpec()
                            .withSchedulerName("kai-scheduler")
                            .withPriorityClassName(workload.getPriorityClass().dbValue)
                            .withVolumes(podVolumes)
                            .withContainers(new ContainerBuilder()
                                    .withName(k8sName)
                                    .withImage(workload.getImage())
                                    .withArgs(args)
                                    .withEnv(env)
                                    .withPorts(new ContainerPortBuilder()
                                            .withName("http")
                                            .withContainerPort(VLLM_CONTAINER_PORT)
                                            .build())
                                    .withResources(resources)
                                    .withVolumeMounts(containerMounts)
                                    .build())
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();
    }

    private LlmInferenceExtra parseExtra(String extraJson) {
        if (extraJson == null || extraJson.isBlank()) {
            throw new IllegalArgumentException("extra JSON is required for LLM_INFERENCE workloads");
        }
        try {
            return objectMapper.readValue(extraJson, LlmInferenceExtra.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("malformed LLM extra JSON: " + e.getOriginalMessage(), e);
        }
    }

    private List<String> tokenizeVllmParams(String vllmParams) {
        if (vllmParams == null || vllmParams.isBlank()) {
            return List.of();
        }
        return Arrays.stream(vllmParams.trim().split("\\s+"))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private List<EnvVar> buildEnv(List<LlmInferenceExtra.EnvVar> envVars) {
        if (envVars == null || envVars.isEmpty()) {
            return List.of();
        }
        return envVars.stream()
                .filter(ev -> ev.key() != null && !ev.key().isBlank())
                .map(ev -> new EnvVarBuilder()
                        .withName(ev.key())
                        .withValue(ev.value() == null ? "" : ev.value())
                        .build())
                .toList();
    }

    private ResourceRequirements buildResources(Workload workload) {
        Map<String, Quantity> requests = new HashMap<>();
        Map<String, Quantity> limits = new HashMap<>();

        if (workload.getRequestedCpu() != null) {
            requests.put("cpu", new Quantity(workload.getRequestedCpu().toPlainString()));
        }
        if (workload.getRequestedMemory() != null) {
            requests.put("memory", new Quantity(workload.getRequestedMemory() + "Mi"));
        }
        if (workload.getRequestedGpu() != null) {
            Quantity gpu = new Quantity(workload.getRequestedGpu().toPlainString());
            requests.put("nvidia.com/gpu", gpu);
            limits.put("nvidia.com/gpu", gpu);
        }

        return new ResourceRequirementsBuilder()
                .withRequests(requests)
                .withLimits(limits)
                .build();
    }
}
