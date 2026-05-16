package com.trucdnd.gpu_hub_backend.workload_source.controller;

import com.trucdnd.gpu_hub_backend.workload_source.dto.AttachSourceRequest;
import com.trucdnd.gpu_hub_backend.workload_source.dto.WorkloadSourceDto;
import com.trucdnd.gpu_hub_backend.workload_source.service.WorkloadSourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workloads/{workloadId}/sources")
@RequiredArgsConstructor
public class WorkloadSourceController {

    private final WorkloadSourceService workloadSourceService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @rbac.canAccessWorkload(#workloadId)")
    public ResponseEntity<List<WorkloadSourceDto>> list(@PathVariable UUID workloadId) {
        return ResponseEntity.ok(workloadSourceService.findByWorkload(workloadId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @rbac.canAccessWorkload(#workloadId)")
    public ResponseEntity<WorkloadSourceDto> attach(@PathVariable UUID workloadId,
            @RequestBody @Valid AttachSourceRequest request) {
        return ResponseEntity.ok(workloadSourceService.attach(workloadId, request));
    }

    @DeleteMapping("/{sourceId}")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canAccessWorkload(#workloadId)")
    public ResponseEntity<Void> detach(@PathVariable UUID workloadId, @PathVariable UUID sourceId) {
        workloadSourceService.detach(workloadId, sourceId);
        return ResponseEntity.noContent().build();
    }
}
