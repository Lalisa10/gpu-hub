package com.trucdnd.gpu_hub_backend.workload.event;

import com.trucdnd.gpu_hub_backend.common.constants.Workload.Status;

import java.util.UUID;

public record WorkloadStatusChangedEvent(UUID workloadId, Status oldStatus, Status newStatus) {
}
