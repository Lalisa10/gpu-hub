package com.trucdnd.gpu_hub_backend.workload.dto;

/** Spec for mounting a folder of the team PVC into a workload pod via subPath. */
public record VolumeMountSpec(String pvcName, String folderName, String mountPath, boolean readOnly) {
}
