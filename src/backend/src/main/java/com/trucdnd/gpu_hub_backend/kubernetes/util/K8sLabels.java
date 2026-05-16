package com.trucdnd.gpu_hub_backend.kubernetes.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class K8sLabels {

    public static final String OWNER       = "gpu-hub/owner";
    public static final String TEAM        = "gpu-hub/team";
    public static final String CLUSTER     = "gpu-hub/cluster";
    public static final String CREATED_BY  = "gpu-hub/created-by";
    public static final String PROJECT     = "gpu-hub/project";
    public static final String DATA_SOURCE = "gpu-hub/data-source";

    public static final String WORKLOAD_ID     = "gpu-hub/workload-id";
    public static final String DATA_SOURCE_ID  = "gpu-hub/data-source-id";
    public static final String TEAM_CLUSTER_ID = "gpu-hub/team-cluster-id";

    public static final String OWNER_TEAM_PVC      = "team-pvc";
    public static final String OWNER_DATA_SOURCE   = "data-source";
    public static final String OWNER_WORKLOAD      = "workload";
    public static final String OWNER_TEAM_QUEUE    = "team-queue";
    public static final String OWNER_PROJECT_QUEUE = "project-queue";

    private K8sLabels() {}

    public static Map<String, String> standard(String owner, String teamName, String clusterName, String createdByUsername) {
        Map<String, String> labels = new LinkedHashMap<>();
        if (owner != null) labels.put(OWNER, sanitize(owner));
        if (teamName != null) labels.put(TEAM, sanitize(teamName));
        if (clusterName != null) labels.put(CLUSTER, sanitize(clusterName));
        if (createdByUsername != null) labels.put(CREATED_BY, sanitize(createdByUsername));
        return labels;
    }

    public static String sanitize(String value) {
        if (value == null) return "";
        String lowered = value.toLowerCase()
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^[-._]+|[-._]+$", "");
        if (lowered.length() > 63) lowered = lowered.substring(0, 63).replaceAll("[-._]+$", "");
        return lowered;
    }
}
