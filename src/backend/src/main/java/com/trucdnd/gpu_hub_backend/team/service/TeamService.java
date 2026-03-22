package com.trucdnd.gpu_hub_backend.team.service;

import com.trucdnd.gpu_hub_backend.team.dto.CreateTeamRequest;
import com.trucdnd.gpu_hub_backend.team.dto.PatchTeamRequest;
import com.trucdnd.gpu_hub_backend.team.dto.TeamDto;
import com.trucdnd.gpu_hub_backend.team.dto.UpdateTeamRequest;
import com.trucdnd.gpu_hub_backend.team.entity.Team;
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

    public void delete(UUID id) {
        Team team = getTeam(id);
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
