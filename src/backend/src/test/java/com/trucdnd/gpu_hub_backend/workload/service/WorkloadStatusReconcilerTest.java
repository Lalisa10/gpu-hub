package com.trucdnd.gpu_hub_backend.workload.service;

import com.trucdnd.gpu_hub_backend.common.constants.Workload.Status;
import com.trucdnd.gpu_hub_backend.kubernetes.service.BuiltinResourceService;
import com.trucdnd.gpu_hub_backend.team.repository.TeamClusterRepository;
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
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class WorkloadStatusReconcilerTest {

    @Mock private WorkloadRepository workloadRepository;
    @Mock private BuiltinResourceService builtinResourceService;
    @Mock private TeamClusterRepository teamClusterRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private WorkloadStatusReconciler reconciler;

    @BeforeEach
    void setUp() {
        reconciler = new WorkloadStatusReconciler(
                workloadRepository, builtinResourceService, teamClusterRepository, eventPublisher);
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
