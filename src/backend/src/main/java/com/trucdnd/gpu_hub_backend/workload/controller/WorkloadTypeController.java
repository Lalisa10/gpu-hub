package com.trucdnd.gpu_hub_backend.workload.controller;

import com.trucdnd.gpu_hub_backend.workload.dto.CreateWorkloadTypeRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.PatchWorkloadTypeRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.UpdateWorkloadTypeRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.WorkloadTypeDto;
import com.trucdnd.gpu_hub_backend.workload.service.WorkloadTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workload-types")
@RequiredArgsConstructor
public class WorkloadTypeController {
    private final WorkloadTypeService workloadTypeService;

    @GetMapping
    public ResponseEntity<List<WorkloadTypeDto>> getAll() {
        return ResponseEntity.ok(workloadTypeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkloadTypeDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(workloadTypeService.findById(id));
    }

    @PostMapping
    public ResponseEntity<WorkloadTypeDto> create(@RequestBody @Valid CreateWorkloadTypeRequest request) {
        WorkloadTypeDto saved = workloadTypeService.create(request);
        return ResponseEntity.created(URI.create("/api/workload-types/" + saved.id())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkloadTypeDto> update(@PathVariable UUID id, @RequestBody @Valid UpdateWorkloadTypeRequest request) {
        return ResponseEntity.ok(workloadTypeService.update(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WorkloadTypeDto> patch(@PathVariable UUID id, @RequestBody @Valid PatchWorkloadTypeRequest request) {
        return ResponseEntity.ok(workloadTypeService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        workloadTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
