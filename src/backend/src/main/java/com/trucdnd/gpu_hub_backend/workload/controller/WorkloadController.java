package com.trucdnd.gpu_hub_backend.workload.controller;

import com.trucdnd.gpu_hub_backend.workload.dto.CreateWorkloadRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.PodInfoDto;
import com.trucdnd.gpu_hub_backend.workload.dto.WorkloadDto;
import com.trucdnd.gpu_hub_backend.workload.service.WorkloadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workloads")
@RequiredArgsConstructor
public class WorkloadController {
    private final WorkloadService workloadService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WorkloadDto>> getAll() {
        return ResponseEntity.ok(workloadService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkloadDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(workloadService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @rbac.canSubmitWorkload(#request.projectId(), #request.submittedById())")
    public ResponseEntity<WorkloadDto> create(@RequestBody @Valid CreateWorkloadRequest request) {
        WorkloadDto saved = workloadService.create(request);
        return ResponseEntity.created(URI.create("/api/workloads/" + saved.id())).body(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        workloadService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pods")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canAccessWorkload(#id)")
    public ResponseEntity<List<PodInfoDto>> listPods(@PathVariable UUID id) {
        return ResponseEntity.ok(workloadService.listPods(id));
    }

    @GetMapping("/{id}/pods/{podName}/logs")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canAccessWorkload(#id)")
    public ResponseEntity<String> getPodLogs(@PathVariable UUID id, @PathVariable String podName) {
        return ResponseEntity.ok(workloadService.getPodLogs(id, podName));
    }
}
