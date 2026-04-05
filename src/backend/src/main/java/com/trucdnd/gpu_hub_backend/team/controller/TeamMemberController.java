package com.trucdnd.gpu_hub_backend.team.controller;

import com.trucdnd.gpu_hub_backend.team.dto.CreateTeamMemberRequest;
import com.trucdnd.gpu_hub_backend.team.dto.PatchTeamMemberRequest;
import com.trucdnd.gpu_hub_backend.team.dto.TeamMemberDto;
import com.trucdnd.gpu_hub_backend.team.dto.UpdateTeamMemberRequest;
import com.trucdnd.gpu_hub_backend.team.service.TeamMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/team-members")
@RequiredArgsConstructor
public class TeamMemberController {
    private final TeamMemberService teamMemberService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TeamMemberDto>> getAll() {
        return ResponseEntity.ok(teamMemberService.findAll());
    }

    @GetMapping("/{teamId}/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageTeam(#teamId)")
    public ResponseEntity<TeamMemberDto> getById(@PathVariable UUID teamId, @PathVariable UUID userId) {
        return ResponseEntity.ok(teamMemberService.findById(teamId, userId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageTeam(#request.teamId())")
    public ResponseEntity<TeamMemberDto> create(@RequestBody @Valid CreateTeamMemberRequest request) {
        return ResponseEntity.ok(teamMemberService.create(request));
    }

    @PutMapping("/{teamId}/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageTeam(#teamId)")
    public ResponseEntity<TeamMemberDto> update(
            @PathVariable UUID teamId,
            @PathVariable UUID userId,
            @RequestBody @Valid UpdateTeamMemberRequest request) {
        return ResponseEntity.ok(teamMemberService.update(teamId, userId, request));
    }

    @PatchMapping("/{teamId}/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageTeam(#teamId)")
    public ResponseEntity<TeamMemberDto> patch(
            @PathVariable UUID teamId,
            @PathVariable UUID userId,
            @RequestBody @Valid PatchTeamMemberRequest request) {
        return ResponseEntity.ok(teamMemberService.patch(teamId, userId, request));
    }

    @DeleteMapping("/{teamId}/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageTeam(#teamId)")
    public ResponseEntity<Void> delete(@PathVariable UUID teamId, @PathVariable UUID userId) {
        teamMemberService.delete(teamId, userId);
        return ResponseEntity.noContent().build();
    }
}
