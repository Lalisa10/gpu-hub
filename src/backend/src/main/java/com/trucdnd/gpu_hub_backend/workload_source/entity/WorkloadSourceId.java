package com.trucdnd.gpu_hub_backend.workload_source.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class WorkloadSourceId implements Serializable {
    private UUID workload;
    private UUID source;

    public WorkloadSourceId() {}

    public WorkloadSourceId(UUID workload, UUID source) {
        this.workload = workload;
        this.source = source;
    }

    public UUID getWorkload() { return workload; }
    public void setWorkload(UUID workload) { this.workload = workload; }
    public UUID getSource() { return source; }
    public void setSource(UUID source) { this.source = source; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkloadSourceId that)) return false;
        return Objects.equals(workload, that.workload) && Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workload, source);
    }
}
