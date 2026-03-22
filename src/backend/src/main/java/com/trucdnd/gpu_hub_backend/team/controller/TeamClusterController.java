package com.trucdnd.gpu_hub_backend.team.controller;

import com.trucdnd.gpu_hub_backend.team.dto.CreateTeamClusterRequest;
import com.trucdnd.gpu_hub_backend.team.dto.PatchTeamClusterRequest;
import com.trucdnd.gpu_hub_backend.team.dto.TeamClusterDto;
import com.trucdnd.gpu_hub_backend.team.dto.UpdateTeamClusterRequest;
import com.trucdnd.gpu_hub_backend.team.service.TeamClusterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/team-clusters")
@RequiredArgsConstructor
public class TeamClusterController {
    private final TeamClusterService teamClusterService;

    @GetMapping
    public ResponseEntity<List<TeamClusterDto>> getAll() {
        return ResponseEntity.ok(teamClusterService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamClusterDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(teamClusterService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TeamClusterDto> create(@RequestBody @Valid CreateTeamClusterRequest request) {
        TeamClusterDto saved = teamClusterService.create(request);
        return ResponseEntity.created(URI.create("/api/team-clusters/" + saved.id())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamClusterDto> update(@PathVariable UUID id, @RequestBody @Valid UpdateTeamClusterRequest request) {
        return ResponseEntity.ok(teamClusterService.update(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TeamClusterDto> patch(@PathVariable UUID id, @RequestBody @Valid PatchTeamClusterRequest request) {
        return ResponseEntity.ok(teamClusterService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        teamClusterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
