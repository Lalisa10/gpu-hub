package com.trucdnd.gpu_hub_backend.common.security;

import com.trucdnd.gpu_hub_backend.common.constants.Team.TeamRole;
import com.trucdnd.gpu_hub_backend.common.constants.User.GlobalRole;
import com.trucdnd.gpu_hub_backend.data_source.repository.DataSourceRepository;
import com.trucdnd.gpu_hub_backend.data_volume.repository.DataVolumeRepository;
import com.trucdnd.gpu_hub_backend.project.repository.ProjectRepository;
import com.trucdnd.gpu_hub_backend.team.repository.TeamMemberRepository;
import com.trucdnd.gpu_hub_backend.workload.entity.Workload;
import com.trucdnd.gpu_hub_backend.workload.repository.WorkloadRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("rbac")
public class RbacService {

    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRepository projectRepository;
    private final WorkloadRepository workloadRepository;
    private final DataVolumeRepository dataVolumeRepository;
    private final DataSourceRepository dataSourceRepository;

    public RbacService(TeamMemberRepository teamMemberRepository,
                       ProjectRepository projectRepository,
                       WorkloadRepository workloadRepository,
                       DataVolumeRepository dataVolumeRepository,
                       DataSourceRepository dataSourceRepository) {
        this.teamMemberRepository = teamMemberRepository;
        this.projectRepository = projectRepository;
        this.workloadRepository = workloadRepository;
        this.dataVolumeRepository = dataVolumeRepository;
        this.dataSourceRepository = dataSourceRepository;
    }

    public boolean canManageTeam(UUID teamId) {
        if (isAdmin()) {
            return true;
        }
        return teamMemberRepository.existsById_UserIdAndId_TeamIdAndRole(
                currentUserId(),
                teamId,
                TeamRole.TEAM_LEAD
        );
    }

    public boolean canManageProjectByTeam(UUID teamId) {
        return canManageTeam(teamId);
    }

    public boolean canManageProject(UUID projectId) {
        if (isAdmin()) {
            return true;
        }
        UUID teamId = projectRepository.findTeamIdByProjectId(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));
        return canManageTeam(teamId);
    }

    public boolean canManagePolicyCreate(UUID clusterId) {
        if (isAdmin()) {
            return true;
        }
        return projectRepository.existsLeadOwnedProjectInCluster(currentUserId(), clusterId);
    }

    public boolean canManagePolicy(UUID policyId) {
        if (isAdmin()) {
            return true;
        }
        return projectRepository.existsLeadOwnedProjectUsingPolicy(currentUserId(), policyId);
    }

    public boolean canSubmitWorkload(UUID projectId, UUID submittedById) {
        if (isAdmin()) {
            return true;
        }
        UUID currentUserId = currentUserId();
        if (!currentUserId.equals(submittedById)) {
            return false;
        }

        UUID teamId = projectRepository.findTeamIdByProjectId(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));
        return teamMemberRepository.existsById_UserIdAndId_TeamId(currentUserId, teamId);
    }

    public boolean canAccessWorkload(UUID workloadId) {
        if (isAdmin()) return true;
        UUID currentUserId = currentUserId();

        Workload workload = workloadRepository.findById(workloadId)
                .orElseThrow(() -> new EntityNotFoundException("Workload not found: " + workloadId));

        if (workload.getSubmittedBy().getId().equals(currentUserId)) return true;

        UUID teamId = projectRepository.findTeamIdByProjectId(workload.getProject().getId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found for workload: " + workloadId));
        return teamMemberRepository.existsById_UserIdAndId_TeamIdAndRole(currentUserId, teamId, TeamRole.TEAM_LEAD);
    }

    public boolean canCancelWorkload(UUID workloadId) {
        return canAccessWorkload(workloadId);
    }

    public boolean canManageDataVolume(UUID volumeId) {
        if (isAdmin()) {
            return true;
        }
        UUID teamId = dataVolumeRepository.findTeamIdByVolumeId(volumeId)
                .orElseThrow(() -> new EntityNotFoundException("DataVolume not found: " + volumeId));
        return canManageTeam(teamId);
    }

    public boolean canManageDataSource(UUID sourceId) {
        if (isAdmin()) {
            return true;
        }
        UUID teamId = dataSourceRepository.findTeamIdBySourceId(sourceId)
                .orElseThrow(() -> new EntityNotFoundException("DataSource not found: " + sourceId));
        return canManageTeam(teamId);
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return principal.getUserId();
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return principal.getGlobalRole() == GlobalRole.ADMIN;
    }
}
