package com.trucdnd.gpu_hub_backend.team.service;

import com.trucdnd.gpu_hub_backend.team.dto.CreateTeamMemberRequest;
import com.trucdnd.gpu_hub_backend.team.dto.PatchTeamMemberRequest;
import com.trucdnd.gpu_hub_backend.team.dto.TeamMemberDto;
import com.trucdnd.gpu_hub_backend.team.dto.UpdateTeamMemberRequest;
import com.trucdnd.gpu_hub_backend.team.entity.Team;
import com.trucdnd.gpu_hub_backend.team.entity.TeamMember;
import com.trucdnd.gpu_hub_backend.team.entity.TeamMemberId;
import com.trucdnd.gpu_hub_backend.team.repository.TeamMemberRepository;
import com.trucdnd.gpu_hub_backend.team.repository.TeamRepository;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import com.trucdnd.gpu_hub_backend.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamMemberService {
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public List<TeamMemberDto> findAll() {
        return teamMemberRepository.findAll().stream().map(this::toDto).toList();
    }

    public TeamMemberDto findById(UUID teamId, UUID userId) {
        return toDto(getTeamMember(teamId, userId));
    }

    public TeamMemberDto create(CreateTeamMemberRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.userId()));
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + request.teamId()));

        TeamMember entity = new TeamMember();
        entity.setId(new TeamMemberId(request.userId(), request.teamId()));
        entity.setUser(user);
        entity.setTeam(team);
        entity.setJoinedAt(request.joinedAt() != null ? request.joinedAt() : OffsetDateTime.now());

        return toDto(teamMemberRepository.save(entity));
    }

    public TeamMemberDto update(UUID teamId, UUID userId, UpdateTeamMemberRequest request) {
        TeamMember teamMember = getTeamMember(teamId, userId);
        teamMember.setJoinedAt(request.joinedAt());
        return toDto(teamMemberRepository.save(teamMember));
    }

    public TeamMemberDto patch(UUID teamId, UUID userId, PatchTeamMemberRequest request) {
        TeamMember teamMember = getTeamMember(teamId, userId);

        if (request.joinedAt().isPresent()) {
            teamMember.setJoinedAt(request.joinedAt().orElse(null));
        }

        return toDto(teamMemberRepository.save(teamMember));
    }

    public void delete(UUID teamId, UUID userId) {
        teamMemberRepository.delete(getTeamMember(teamId, userId));
    }

    private TeamMember getTeamMember(UUID teamId, UUID userId) {
        TeamMemberId id = new TeamMemberId(userId, teamId);
        return teamMemberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "TeamMember not found with teamId: " + teamId + " and userId: " + userId));
    }

    private TeamMemberDto toDto(TeamMember entity) {
        return new TeamMemberDto(entity.getUser().getId(), entity.getTeam().getId(), entity.getJoinedAt());
    }
}
