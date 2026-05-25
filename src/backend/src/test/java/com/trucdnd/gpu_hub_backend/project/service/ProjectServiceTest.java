package com.trucdnd.gpu_hub_backend.project.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.kubernetes.service.QueueService;
import com.trucdnd.gpu_hub_backend.policy.service.QueueSpecBuilder;
import com.trucdnd.gpu_hub_backend.project.entity.Project;
import com.trucdnd.gpu_hub_backend.project.repository.ProjectRepository;
import com.trucdnd.gpu_hub_backend.team.entity.Team;
import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;
import com.trucdnd.gpu_hub_backend.team.repository.TeamClusterRepository;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;
import com.trucdnd.gpu_hub_backend.workload.repository.WorkloadRepository;
import com.trucdnd.gpu_hub_backend.workload.service.WorkloadService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private TeamClusterRepository teamClusterRepository;
    @Mock private WorkloadRepository workloadRepository;
    @Mock private WorkloadService workloadService;
    @Mock private QueueService queueService;
    @Mock private QueueSpecBuilder queueSpecBuilder;

    @InjectMocks private ProjectService projectService;

    @Test
    void delete_cascadesWorkloads_thenQueue_thenProjectRow() {
        UUID teamId = UUID.randomUUID();
        UUID clusterId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        Team team = new Team();
        team.setId(teamId);
        Cluster cluster = new Cluster();
        cluster.setId(clusterId);
        Project project = Project.builder().team(team).cluster(cluster).name("research").build();
        project.setId(projectId);

        Workload w1 = new Workload();
        w1.setId(UUID.randomUUID());
        Workload w2 = new Workload();
        w2.setId(UUID.randomUUID());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(workloadRepository.findByProject_Id(projectId)).thenReturn(List.of(w1, w2));
        when(teamClusterRepository.findByTeam_IdAndCluster_Id(teamId, clusterId))
                .thenReturn(Optional.of(new TeamCluster()));
        when(queueSpecBuilder.buildProjectQueueName(project)).thenReturn("research");

        projectService.delete(projectId);

        verify(workloadService).delete(w1.getId());
        verify(workloadService).delete(w2.getId());
        verify(queueService).delete(eq(cluster), isNull(), eq("research"));
        verify(projectRepository).delete(project);
    }
}
