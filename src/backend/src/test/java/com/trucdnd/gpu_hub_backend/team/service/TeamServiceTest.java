package com.trucdnd.gpu_hub_backend.team.service;

import com.trucdnd.gpu_hub_backend.data_source.entity.DataSource;
import com.trucdnd.gpu_hub_backend.data_source.repository.DataSourceRepository;
import com.trucdnd.gpu_hub_backend.data_source.service.DataSourceService;
import com.trucdnd.gpu_hub_backend.project.entity.Project;
import com.trucdnd.gpu_hub_backend.project.repository.ProjectRepository;
import com.trucdnd.gpu_hub_backend.project.service.ProjectService;
import com.trucdnd.gpu_hub_backend.team.entity.Team;
import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;
import com.trucdnd.gpu_hub_backend.team.repository.TeamClusterRepository;
import com.trucdnd.gpu_hub_backend.team.repository.TeamRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private DataSourceRepository dataSourceRepository;
    @Mock private TeamClusterRepository teamClusterRepository;
    @Mock private ProjectService projectService;
    @Mock private DataSourceService dataSourceService;
    @Mock private TeamClusterService teamClusterService;

    @InjectMocks private TeamService teamService;

    @Test
    void delete_cascadesProjects_dataSources_teamClusters_thenTeamRow() {
        UUID teamId = UUID.randomUUID();
        Team team = new Team();
        team.setId(teamId);

        Project project = new Project();
        project.setId(UUID.randomUUID());
        DataSource source = new DataSource();
        source.setId(UUID.randomUUID());
        TeamCluster tc = new TeamCluster();
        tc.setId(UUID.randomUUID());

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(projectRepository.findByTeam_Id(teamId)).thenReturn(List.of(project));
        when(dataSourceRepository.findByTeam_Id(teamId)).thenReturn(List.of(source));
        when(teamClusterRepository.findByTeam_Id(teamId)).thenReturn(List.of(tc));

        teamService.delete(teamId);

        // Dependency order matters: projects (cascade their workloads) → data sources →
        // team-clusters (namespace/PVC/queue) → the team row last.
        InOrder order = inOrder(projectService, dataSourceService, teamClusterService, teamRepository);
        order.verify(projectService).delete(project.getId());
        order.verify(dataSourceService).delete(source.getId());
        order.verify(teamClusterService).delete(tc.getId());
        order.verify(teamRepository).delete(team);
    }
}
