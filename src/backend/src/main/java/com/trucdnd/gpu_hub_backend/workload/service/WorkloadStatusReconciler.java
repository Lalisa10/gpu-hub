package com.trucdnd.gpu_hub_backend.workload.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.common.constants.Workload.Status;
import com.trucdnd.gpu_hub_backend.common.constants.Workload.Type;
import com.trucdnd.gpu_hub_backend.kubernetes.service.BuiltinResourceService;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;
import com.trucdnd.gpu_hub_backend.workload.event.WorkloadStatusChangedEvent;
import com.trucdnd.gpu_hub_backend.workload.repository.WorkloadRepository;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkloadStatusReconciler {


    private static final Set<Status> TERMINAL = EnumSet.of(
            Status.SUCCEEDED, Status.FAILED, Status.CANCELLED, Status.PREEMPTED);

    private static final Set<Status> ABSORBING = EnumSet.of(
            Status.SUCCEEDED, Status.FAILED, Status.CANCELLED);

    private static final Set<Type> LONG_RUNNING = EnumSet.of(Type.NOTEBOOK, Type.LLM_INFERENCE);

    private static final String PREEMPTED_REASON = "Preempted";

    private final WorkloadRepository workloadRepository;
    private final BuiltinResourceService builtinResourceService;
    private final ApplicationEventPublisher eventPublisher;

    private final ObjectProvider<WorkloadStatusReconciler> self;

    public void onPodEvent(Cluster cluster, Pod pod) {
        String workloadIdLabel = pod.getMetadata() != null && pod.getMetadata().getLabels() != null
                ? pod.getMetadata().getLabels().get(NotebookSpecBuilder.WORKLOAD_ID_LABEL)
                : null;
        if (workloadIdLabel == null) return;

        UUID workloadId;
        try {
            workloadId = UUID.fromString(workloadIdLabel);
        } catch (IllegalArgumentException e) {
            log.debug("Skipping pod with non-UUID workload-id label: {}", workloadIdLabel);
            return;
        }

        String namespace = pod.getMetadata() != null ? pod.getMetadata().getNamespace() : null;
        if (namespace == null) {
            log.warn("Pod for workload {} has no namespace; skipping", workloadId);
            return;
        }

        List<Pod> pods = builtinResourceService.listPodsByLabel(
                cluster, namespace,
                Map.of(NotebookSpecBuilder.WORKLOAD_ID_LABEL, workloadId.toString()));

        Status target = computeStatus(pods);
        self.getObject().applyStatus(workloadId, target);
    }

    public Status computeStatus(List<Pod> pods) {
        if (pods.isEmpty()) return Status.PENDING;

        boolean anyPreempted = false;
        boolean anyFailed = false;
        boolean anyRunningReady = false;
        boolean allSucceeded = true;

        for (Pod pod : pods) {
            String phase = pod.getStatus() != null ? pod.getStatus().getPhase() : null;
            if (phase == null) {
                allSucceeded = false;
                continue;
            }
            if (!"Succeeded".equals(phase)) allSucceeded = false;

            if (isPreempted(pod)) {
                anyPreempted = true;
            } else if ("Failed".equals(phase)) {
                anyFailed = true;
            } else if ("Running".equals(phase) && hasReadyContainer(pod)) {
                anyRunningReady = true;
            }
        }

        if (anyPreempted) return Status.PREEMPTED;
        if (anyFailed) return Status.FAILED;
        if (anyRunningReady) return Status.RUNNING;
        if (allSucceeded) return Status.SUCCEEDED;
        return Status.PENDING;
    }

    private boolean isPreempted(Pod pod) {
        if (pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) return false;
        for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
            if (cs.getState() != null && cs.getState().getTerminated() != null
                    && PREEMPTED_REASON.equals(cs.getState().getTerminated().getReason())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasReadyContainer(Pod pod) {
        if (pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) return false;
        for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
            if (Boolean.TRUE.equals(cs.getReady())) return true;
        }
        return false;
    }

    @Transactional
    public void applyStatus(UUID workloadId, Status target) {
        Workload workload = workloadRepository.findById(workloadId).orElse(null);
        if (workload == null) {
            log.debug("applyStatus for unknown workload {}", workloadId);
            return;
        }

        Status current = workload.getStatus();
        if (current == target) return;
        if (ABSORBING.contains(current)) return;

        if (current == Status.RUNNING && target == Status.PENDING) {
            target = Status.PREEMPTED;
        }

        if (current == Status.PREEMPTED && target == Status.PENDING) return;

        if (target == Status.SUCCEEDED && LONG_RUNNING.contains(workload.getWorkloadType())) {
            return;
        }

        workload.setStatus(target);
        OffsetDateTime now = OffsetDateTime.now();
        if (target == Status.RUNNING && workload.getStartedAt() == null) {
            workload.setStartedAt(now);
        }

        if (current == Status.PREEMPTED && target == Status.RUNNING) {
            workload.setFinishedAt(null);
        }
        if (TERMINAL.contains(target) && workload.getFinishedAt() == null) {
            workload.setFinishedAt(now);
        }
        workloadRepository.save(workload);
        eventPublisher.publishEvent(new WorkloadStatusChangedEvent(workload.getId(), current, target));
        log.info("Workload {} status: {} -> {}", workload.getId(), current, target);
    }
}
