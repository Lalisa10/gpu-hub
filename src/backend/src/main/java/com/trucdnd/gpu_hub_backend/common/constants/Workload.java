package com.trucdnd.gpu_hub_backend.common.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Workload {
    private Workload() {}

    public enum Type {
        NOTEBOOK("notebook"),
        LLM_INFERENCE("llm_inference");

        @JsonValue
        public final String dbValue;
        Type(String dbValue) { this.dbValue = dbValue; }

        @JsonCreator
        public static Type fromDbValue(String v) {
            for (Type t : values()) if (t.dbValue.equals(v)) return t;
            throw new IllegalArgumentException("Unknown workload type: " + v);
        }
    }

    public enum Status {
        PENDING("pending"),
        RUNNING("running"),
        SUCCEEDED("succeeded"),
        FAILED("failed"),
        PREEMPTED("preempted"),
        CANCELLED("cancelled");

        @JsonValue
        public final String dbValue;
        Status(String dbValue) { this.dbValue = dbValue; }

        @JsonCreator
        public static Status fromDbValue(String v) {
            for (Status s : values()) if (s.dbValue.equals(v)) return s;
            throw new IllegalArgumentException("Unknown workload status: " + v);
        }
    }

    public enum PriorityClass {
        TRAIN("train"),
        BUILD_PREEMPTIBLE("build-preemptible"),
        BUILD("build"),
        INFERENCE("inference");

        @JsonValue
        public final String dbValue;
        PriorityClass(String dbValue) { this.dbValue = dbValue; }

        @JsonCreator
        public static PriorityClass fromDbValue(String v) {
            for (PriorityClass p : values()) if (p.dbValue.equals(v)) return p;
            throw new IllegalArgumentException("Unknown priority class: " + v);
        }
    }
}
