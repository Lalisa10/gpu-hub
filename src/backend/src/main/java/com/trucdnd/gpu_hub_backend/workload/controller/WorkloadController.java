package com.trucdnd.gpu_hub_backend.workload.controller;

import com.trucdnd.gpu_hub_backend.workload.dto.CreateWorkloadRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.PatchWorkloadRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.UpdateWorkloadRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.WorkloadDto;
import com.trucdnd.gpu_hub_backend.workload.service.WorkloadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<WorkloadDto>> getAll() {
        return ResponseEntity.ok(workloadService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkloadDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(workloadService.findById(id));
    }

    @PostMapping
    public ResponseEntity<WorkloadDto> create(@RequestBody @Valid CreateWorkloadRequest request) {
        WorkloadDto saved = workloadService.create(request);
        return ResponseEntity.created(URI.create("/api/workloads/" + saved.id())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkloadDto> update(@PathVariable UUID id, @RequestBody @Valid UpdateWorkloadRequest request) {
        return ResponseEntity.ok(workloadService.update(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WorkloadDto> patch(@PathVariable UUID id, @RequestBody @Valid PatchWorkloadRequest request) {
        return ResponseEntity.ok(workloadService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        workloadService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
