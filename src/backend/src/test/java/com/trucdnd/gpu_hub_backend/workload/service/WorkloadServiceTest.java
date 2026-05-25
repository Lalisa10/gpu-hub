package com.trucdnd.gpu_hub_backend.workload.service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.common.constants.Workload.Status;
import com.trucdnd.gpu_hub_backend.common.constants.Workload.Type;
import com.trucdnd.gpu_hub_backend.kubernetes.service.BuiltinResourceService;
import com.trucdnd.gpu_hub_backend.kubernetes.service.NotebookService;
import com.trucdnd.gpu_hub_backend.project.entity.Project;
import com.trucdnd.gpu_hub_backend.team.entity.Team;
import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;
import com.trucdnd.gpu_hub_backend.team.repository.TeamClusterRepository;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;
import com.trucdnd.gpu_hub_backend.workload.repository.WorkloadRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkloadServiceTest {

    @Mock private WorkloadRepository workloadRepository;
    @Mock private TeamClusterRepository teamClusterRepository;
    @Mock private NotebookService notebookService;
    @Mock private BuiltinResourceService builtinResourceService;

    @InjectMocks private WorkloadService workloadService;

    @Test
    void delete_notebook_tearsDownNotebookCr_thenHardDeletesRow() {
        UUID teamId = UUID.randomUUID();
        UUID clusterId = UUID.randomUUID();
        UUID workloadId = UUID.randomUUID();

        Team team = new Team();
        team.setId(teamId);
        Cluster cluster = new Cluster();
        cluster.setId(clusterId);
        Project project = Project.builder().team(team).cluster(cluster).name("p").build();

        Workload workload = Workload.builder()
                .project(project)
                .cluster(cluster)
                .workloadType(Type.NOTEBOOK)
                .status(Status.RUNNING)
                .build();
        workload.setId(workloadId);

        when(workloadRepository.findById(workloadId)).thenReturn(Optional.of(workload));
        TeamCluster teamCluster = TeamCluster.builder().namespace("gpuhub-team-x").build();
        when(teamClusterRepository.findByTeam_IdAndCluster_Id(teamId, clusterId))
                .thenReturn(Optional.of(teamCluster));

        workloadService.delete(workloadId);

        verify(notebookService).deleteByLabel(
                cluster, "gpuhub-team-x", NotebookSpecBuilder.WORKLOAD_ID_LABEL, workloadId.toString());
        verify(workloadRepository).delete(workload);
        // NOTEBOOK teardown must not touch the Deployment path.
        verifyNoInteractions(builtinResourceService);
    }
}
