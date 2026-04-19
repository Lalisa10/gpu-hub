package com.trucdnd.gpu_hub_backend.workload.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmInferenceExtra(
        String modelSource,
        String vllmParams,
        Integer replicaCount,
        List<EnvVar> envVars
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnvVar(String key, String value) {}
}
