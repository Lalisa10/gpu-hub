package com.trucdnd.gpu_hub_backend.workload_volume.controller;

import com.trucdnd.gpu_hub_backend.workload_volume.dto.AttachVolumeRequest;
import com.trucdnd.gpu_hub_backend.workload_volume.dto.WorkloadVolumeDto;
import com.trucdnd.gpu_hub_backend.workload_volume.service.WorkloadVolumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workloads/{workloadId}/volumes")
@RequiredArgsConstructor
public class WorkloadVolumeController {
    private final WorkloadVolumeService workloadVolumeService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @rbac.canAccessWorkload(#workloadId)")
    public ResponseEntity<List<WorkloadVolumeDto>> list(@PathVariable UUID workloadId) {
        return ResponseEntity.ok(workloadVolumeService.findByWorkload(workloadId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @rbac.canAccessWorkload(#workloadId)")
    public ResponseEntity<WorkloadVolumeDto> attach(
            @PathVariable UUID workloadId,
            @RequestBody @Valid AttachVolumeRequest request) {
        return ResponseEntity.ok(workloadVolumeService.attach(workloadId, request));
    }

    @DeleteMapping("/{volumeId}")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canAccessWorkload(#workloadId)")
    public ResponseEntity<Void> detach(@PathVariable UUID workloadId, @PathVariable UUID volumeId) {
        workloadVolumeService.detach(workloadId, volumeId);
        return ResponseEntity.noContent().build();
    }
}
