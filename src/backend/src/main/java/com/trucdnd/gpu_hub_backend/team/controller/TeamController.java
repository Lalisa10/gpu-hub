package com.trucdnd.gpu_hub_backend.team.controller;

import com.trucdnd.gpu_hub_backend.team.dto.CreateTeamRequest;
import com.trucdnd.gpu_hub_backend.team.dto.PatchTeamRequest;
import com.trucdnd.gpu_hub_backend.team.dto.TeamDto;
import com.trucdnd.gpu_hub_backend.team.dto.UpdateTeamRequest;
import com.trucdnd.gpu_hub_backend.team.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;

    @GetMapping
    public ResponseEntity<List<TeamDto>> getAll() {
        return ResponseEntity.ok(teamService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(teamService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TeamDto> create(@RequestBody @Valid CreateTeamRequest request) {
        TeamDto saved = teamService.create(request);
        return ResponseEntity.created(URI.create("/api/teams/" + saved.id())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamDto> update(@PathVariable UUID id, @RequestBody @Valid UpdateTeamRequest request) {
        return ResponseEntity.ok(teamService.update(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TeamDto> patch(@PathVariable UUID id, @RequestBody @Valid PatchTeamRequest request) {
        return ResponseEntity.ok(teamService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        teamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
