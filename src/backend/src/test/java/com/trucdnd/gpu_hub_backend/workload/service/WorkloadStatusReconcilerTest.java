package com.trucdnd.gpu_hub_backend.workload.service;

import com.trucdnd.gpu_hub_backend.common.constants.Workload.Status;
import com.trucdnd.gpu_hub_backend.common.constants.Workload.Type;
import com.trucdnd.gpu_hub_backend.kubernetes.service.BuiltinResourceService;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;
import com.trucdnd.gpu_hub_backend.workload.repository.WorkloadRepository;

import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkloadStatusReconcilerTest {

    @Mock private WorkloadRepository workloadRepository;
    @Mock private BuiltinResourceService builtinResourceService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ObjectProvider<WorkloadStatusReconciler> self;

    private WorkloadStatusReconciler reconciler;

    @BeforeEach
    void setUp() {
        reconciler = new WorkloadStatusReconciler(
                workloadRepository, builtinResourceService, eventPublisher, self);
    }

    @Test
    void emptyPodList_isPending() {
        assertEquals(Status.PENDING, reconciler.computeStatus(List.of()));
    }

    @Test
    void allSucceeded_isSucceeded() {
        assertEquals(Status.SUCCEEDED, reconciler.computeStatus(List.of(
                pod("Succeeded", false, false),
                pod("Succeeded", false, false))));
    }

    @Test
    void anyPreempted_beatsFailed() {
        assertEquals(Status.PREEMPTED, reconciler.computeStatus(List.of(
                pod("Failed", false, true),
                pod("Failed", false, false))));
    }

    @Test
    void anyFailedNonPreempted_isFailed() {
        assertEquals(Status.FAILED, reconciler.computeStatus(List.of(
                pod("Running", true, false),
                pod("Failed", false, false))));
    }

    @Test
    void runningWithReadyContainer_isRunning() {
        assertEquals(Status.RUNNING, reconciler.computeStatus(List.of(
                pod("Running", true, false))));
    }

    @Test
    void runningWithoutReadyContainer_isPending() {
        assertEquals(Status.PENDING, reconciler.computeStatus(List.of(
                pod("Running", false, false))));
    }

    @Test
    void pendingPhase_isPending() {
        assertEquals(Status.PENDING, reconciler.computeStatus(List.of(
                pod("Pending", false, false))));
    }

    @Test
    void preemptedTakesPriorityOverRunning() {
        assertEquals(Status.PREEMPTED, reconciler.computeStatus(List.of(
                pod("Running", true, false),
                pod("Failed", false, true))));
    }

    @Test
    void runningToPending_isPreempted() {
        // KAI evicts the running pod; the recreated pod is Pending with no "Preempted" reason,
        // so computeStatus yields PENDING. A workload that was RUNNING must flip to PREEMPTED.
        UUID id = UUID.randomUUID();
        Workload workload = Workload.builder()
                .status(Status.RUNNING)
                .workloadType(Type.LLM_INFERENCE)
                .build();
        when(workloadRepository.findById(id)).thenReturn(java.util.Optional.of(workload));

        reconciler.applyStatus(id, Status.PENDING);

        assertEquals(Status.PREEMPTED, workload.getStatus());
        assertNotNull(workload.getFinishedAt());
        verify(workloadRepository).save(workload);
    }

    @Test
    void pendingToPending_staysPending_notPreempted() {
        // A workload that never reached RUNNING and is still PENDING must not be treated as preempted.
        UUID id = UUID.randomUUID();
        Workload workload = Workload.builder()
                .status(Status.PENDING)
                .workloadType(Type.NOTEBOOK)
                .build();
        when(workloadRepository.findById(id)).thenReturn(java.util.Optional.of(workload));

        // current == target == PENDING short-circuits; nothing is persisted.
        reconciler.applyStatus(id, Status.PENDING);

        assertEquals(Status.PENDING, workload.getStatus());
        verify(workloadRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void preemptedToRunning_recovers_andClearsFinishedAt() {
        // KAI reschedules the preempted workload and its pod runs again — it must recover to RUNNING
        // and shed the preemption finish timestamp so it no longer looks finished.
        UUID id = UUID.randomUUID();
        Workload workload = Workload.builder()
                .status(Status.PREEMPTED)
                .workloadType(Type.LLM_INFERENCE)
                .finishedAt(OffsetDateTime.now())
                .build();
        when(workloadRepository.findById(id)).thenReturn(java.util.Optional.of(workload));

        reconciler.applyStatus(id, Status.RUNNING);

        assertEquals(Status.RUNNING, workload.getStatus());
        assertNull(workload.getFinishedAt());
        verify(workloadRepository).save(workload);
    }

    @Test
    void preemptedToPending_staysPreempted() {
        // While awaiting reschedule the recreated pod is Pending; the workload keeps showing
        // PREEMPTED rather than flapping back to PENDING.
        UUID id = UUID.randomUUID();
        Workload workload = Workload.builder()
                .status(Status.PREEMPTED)
                .workloadType(Type.NOTEBOOK)
                .build();
        when(workloadRepository.findById(id)).thenReturn(java.util.Optional.of(workload));

        reconciler.applyStatus(id, Status.PENDING);

        assertEquals(Status.PREEMPTED, workload.getStatus());
        verify(workloadRepository, org.mockito.Mockito.never()).save(any());
    }

    private static Pod pod(String phase, boolean ready, boolean preempted) {
        Pod pod = new Pod();
        PodStatus status = new PodStatus();
        status.setPhase(phase);

        ContainerStatus cs = new ContainerStatus();
        cs.setReady(ready);
        if (preempted) {
            ContainerStateTerminated terminated = new ContainerStateTerminated();
            terminated.setReason("Preempted");
            ContainerState state = new ContainerState();
            state.setTerminated(terminated);
            cs.setState(state);
        }
        status.setContainerStatuses(List.of(cs));
        pod.setStatus(status);
        return pod;
    }
}
