package com.trucdnd.gpu_hub_backend.team.service;

import com.trucdnd.gpu_hub_backend.data_source.repository.DataSourceRepository;
import com.trucdnd.gpu_hub_backend.data_source.service.DataSourceService;
import com.trucdnd.gpu_hub_backend.project.repository.ProjectRepository;
import com.trucdnd.gpu_hub_backend.project.service.ProjectService;
import com.trucdnd.gpu_hub_backend.team.dto.CreateTeamRequest;
import com.trucdnd.gpu_hub_backend.team.dto.PatchTeamRequest;
import com.trucdnd.gpu_hub_backend.team.dto.TeamDto;
import com.trucdnd.gpu_hub_backend.team.dto.UpdateTeamRequest;
import com.trucdnd.gpu_hub_backend.team.entity.Team;
import com.trucdnd.gpu_hub_backend.team.entity.TeamCluster;
import com.trucdnd.gpu_hub_backend.team.repository.TeamClusterRepository;
import com.trucdnd.gpu_hub_backend.team.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final DataSourceRepository dataSourceRepository;
    private final TeamClusterRepository teamClusterRepository;
    private final ProjectService projectService;
    private final DataSourceService dataSourceService;
    private final TeamClusterService teamClusterService;

    public List<TeamDto> findAll() {
        return teamRepository.findAll().stream().map(this::toDto).toList();
    }

    public TeamDto findById(UUID id) {
        return toDto(getTeam(id));
    }

    public TeamDto create(CreateTeamRequest request) {
        Team team = Team.builder()
                .name(request.name())
                .description(request.description())
                .build();
        return toDto(teamRepository.save(team));
    }

    public TeamDto update(UUID id, UpdateTeamRequest request) {
        Team team = getTeam(id);
        team.setName(request.name());
        team.setDescription(request.description());
        return toDto(teamRepository.save(team));
    }

    public TeamDto patch(UUID id, PatchTeamRequest request) {
        Team team = getTeam(id);

        if (request.name().isPresent()) {
            team.setName(request.name().orElse(null));
        }
        if (request.description().isPresent()) {
            team.setDescription(request.description().orElse(null));
        }

        return toDto(teamRepository.save(team));
    }

    /**
     * Deletes a team and everything it owns, in dependency order (the DB FKs from workloads and
     * data_sources are {@code NO ACTION}, so the children must be removed before the team row):
     * projects (each cascades its workloads + project queue) → data sources (migration jobs +
     * rows) → team-clusters (namespace + PVC + team queue) → the team row itself. Not
     * {@code @Transactional} — the delegated deletes each manage their own transaction and perform
     * K8s calls that must not pin a JDBC connection.
     */
    public void delete(UUID id) {
        Team team = getTeam(id);

        projectRepository.findByTeam_Id(id)
                .forEach(project -> projectService.delete(project.getId()));
        dataSourceRepository.findByTeam_Id(id)
                .forEach(source -> dataSourceService.delete(source.getId()));
        teamClusterRepository.findByTeam_Id(id)
                .forEach(tc -> teamClusterService.delete(tc.getId()));

        teamRepository.delete(team);
    }

    private Team getTeam(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + id));
    }

    private TeamDto toDto(Team team) {
        return new TeamDto(team.getId(), team.getName(), team.getDescription(), team.getCreatedAt(), team.getUpdatedAt());
    }
}
