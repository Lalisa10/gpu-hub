package com.trucdnd.gpu_hub_backend.project.controller;

import com.trucdnd.gpu_hub_backend.project.dto.CreateProjectRequest;
import com.trucdnd.gpu_hub_backend.project.dto.PatchProjectRequest;
import com.trucdnd.gpu_hub_backend.project.dto.ProjectDto;
import com.trucdnd.gpu_hub_backend.project.dto.UpdateProjectRequest;
import com.trucdnd.gpu_hub_backend.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getAll() {
        return ResponseEntity.ok(projectService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProjectDto> create(@RequestBody @Valid CreateProjectRequest request) {
        ProjectDto saved = projectService.create(request);
        return ResponseEntity.created(URI.create("/api/projects/" + saved.id())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDto> update(@PathVariable UUID id, @RequestBody @Valid UpdateProjectRequest request) {
        return ResponseEntity.ok(projectService.update(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectDto> patch(@PathVariable UUID id, @RequestBody @Valid PatchProjectRequest request) {
        return ResponseEntity.ok(projectService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
