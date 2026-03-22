package com.trucdnd.gpu_hub_backend.project.controller;

import com.trucdnd.gpu_hub_backend.project.dto.CreateProjectClusterPolicyRequest;
import com.trucdnd.gpu_hub_backend.project.dto.PatchProjectClusterPolicyRequest;
import com.trucdnd.gpu_hub_backend.project.dto.ProjectClusterPolicyDto;
import com.trucdnd.gpu_hub_backend.project.dto.UpdateProjectClusterPolicyRequest;
import com.trucdnd.gpu_hub_backend.project.service.ProjectClusterPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/project-cluster-policies")
@RequiredArgsConstructor
public class ProjectClusterPolicyController {
    private final ProjectClusterPolicyService projectClusterPolicyService;

    @GetMapping
    public ResponseEntity<List<ProjectClusterPolicyDto>> getAll() {
        return ResponseEntity.ok(projectClusterPolicyService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectClusterPolicyDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(projectClusterPolicyService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProjectClusterPolicyDto> create(@RequestBody @Valid CreateProjectClusterPolicyRequest request) {
        ProjectClusterPolicyDto saved = projectClusterPolicyService.create(request);
        return ResponseEntity.created(URI.create("/api/project-cluster-policies/" + saved.id())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectClusterPolicyDto> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateProjectClusterPolicyRequest request) {
        return ResponseEntity.ok(projectClusterPolicyService.update(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectClusterPolicyDto> patch(
            @PathVariable UUID id,
            @RequestBody @Valid PatchProjectClusterPolicyRequest request) {
        return ResponseEntity.ok(projectClusterPolicyService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectClusterPolicyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
