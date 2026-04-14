package com.trucdnd.gpu_hub_backend.common.constants;

public class Workload {
    private Workload() {}

    public enum Type {
        NOTEBOOK,
        LLM_INFERENCE
    }

    public enum Status {
        PENDING,
        QUEUED, 
        RUNNING,
        SUCCEEDED,
        FAILED,
        PREEMPTED,
        CANCELLED
    }

    public enum PriorityClass {
        TRAIN,
        BUILD_PREEMPTIBLE,
        BUILD,
        INFERENCE
    }
}
