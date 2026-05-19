package com.trucdnd.gpu_hub_backend.workload.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Subset of the workload {@code extra} JSONB shared by every workload type.
 * Currently only {@code shmSize} — a Kubernetes quantity string (e.g. {@code "2Gi"})
 * for the pod's {@code /dev/shm} shared-memory volume.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkloadExtra(String shmSize) {

    /**
     * Lenient parse: a blank, null, or malformed {@code extra} yields an empty
     * {@code WorkloadExtra(null)} instead of throwing — older workloads have no
     * {@code extra} and notebooks may omit it entirely.
     */
    public static WorkloadExtra parse(String extraJson, ObjectMapper mapper) {
        if (extraJson == null || extraJson.isBlank()) {
            return new WorkloadExtra(null);
        }
        try {
            return mapper.readValue(extraJson, WorkloadExtra.class);
        } catch (Exception e) {
            return new WorkloadExtra(null);
        }
    }

    /** True when a non-blank shared-memory size was requested. */
    public boolean hasShmSize() {
        return shmSize != null && !shmSize.isBlank();
    }
}
