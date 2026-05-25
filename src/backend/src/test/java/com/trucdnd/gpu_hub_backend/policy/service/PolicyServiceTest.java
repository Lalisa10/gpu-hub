package com.trucdnd.gpu_hub_backend.policy.service;

import com.trucdnd.gpu_hub_backend.policy.entity.Policy;
import com.trucdnd.gpu_hub_backend.policy.repository.PolicyRepository;
import com.trucdnd.gpu_hub_backend.project.entity.Project;
import com.trucdnd.gpu_hub_backend.project.repository.ProjectRepository;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.kubernetes.service.QueueService;
import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;
import com.trucdnd.gpu_hub_backend.team.repository.TeamClusterRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock private PolicyRepository policyRepository;
    @Mock private ClusterRepository clusterRepository;
    @Mock private QueueService queueService;
    @Mock private QueueSpecBuilder queueSpecBuilder;
    @Mock private TeamClusterRepository teamClusterRepository;
    @Mock private ProjectRepository projectRepository;

    @InjectMocks private PolicyService policyService;

    @Test
    void delete_whenUsedByTeamCluster_throwsAndDeletesNothing() {
        UUID id = UUID.randomUUID();
        when(policyRepository.findById(id)).thenReturn(Optional.of(new Policy()));
        when(teamClusterRepository.findByPolicy_Id(id)).thenReturn(List.of(new TeamCluster()));

        assertThrows(IllegalStateException.class, () -> policyService.delete(id));

        verify(policyRepository, never()).delete(any());
        verify(queueService, never()).delete(any(), any(), anyString());
    }

    @Test
    void delete_whenUsedByProject_throwsAndDeletesNothing() {
        UUID id = UUID.randomUUID();
        when(policyRepository.findById(id)).thenReturn(Optional.of(new Policy()));
        when(teamClusterRepository.findByPolicy_Id(id)).thenReturn(List.of());
        when(projectRepository.findByPolicy_Id(id)).thenReturn(List.of(new Project()));

        assertThrows(IllegalStateException.class, () -> policyService.delete(id));

        verify(policyRepository, never()).delete(any());
    }

    @Test
    void delete_whenUnused_deletesPolicy() {
        UUID id = UUID.randomUUID();
        Policy policy = new Policy();
        when(policyRepository.findById(id)).thenReturn(Optional.of(policy));
        when(teamClusterRepository.findByPolicy_Id(id)).thenReturn(List.of());
        when(projectRepository.findByPolicy_Id(id)).thenReturn(List.of());

        policyService.delete(id);

        verify(policyRepository).delete(policy);
    }
}
