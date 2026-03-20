package com.trucdnd.gpu_hub_backend.common.constants;

import java.util.concurrent.ExecutionException;

public class Cluster {
    private Cluster() {
        throw new UnsupportedOperationException("This class should never be instantiated");
    }
    public enum Status {
        ACTIVE,
        INACTIVE,
        MAINTENANCE
    }
}

