package com.trucdnd.gpu_hub_backend.data_volume.controller;

import com.trucdnd.gpu_hub_backend.common.constants.User.GlobalRole;
import com.trucdnd.gpu_hub_backend.common.security.UserPrincipal;
import com.trucdnd.gpu_hub_backend.data_volume.dto.CreateDataVolumeRequest;
import com.trucdnd.gpu_hub_backend.data_volume.dto.DataVolumeDto;
import com.trucdnd.gpu_hub_backend.data_volume.dto.PatchDataVolumeRequest;
import com.trucdnd.gpu_hub_backend.data_volume.dto.UpdateDataVolumeRequest;
import com.trucdnd.gpu_hub_backend.data_volume.service.DataVolumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/data-volumes")
@RequiredArgsConstructor
public class DataVolumeController {
    private final DataVolumeService dataVolumeService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DataVolumeDto>> getAll(@RequestParam(required = false) UUID teamId) {
        if (teamId != null) {
            return ResponseEntity.ok(dataVolumeService.findByTeam(teamId));
        }
        return ResponseEntity.ok(dataVolumeService.findAll());
    }

    @GetMapping("/my")
    public ResponseEntity<List<DataVolumeDto>> getMine(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getGlobalRole() == GlobalRole.ADMIN) {
            return ResponseEntity.ok(dataVolumeService.findAll());
        }
        return ResponseEntity.ok(dataVolumeService.findByUser(principal.getUserId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageDataVolume(#id)")
    public ResponseEntity<DataVolumeDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(dataVolumeService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageTeam(#request.teamId())")
    public ResponseEntity<DataVolumeDto> create(@RequestBody @Valid CreateDataVolumeRequest request) {
        DataVolumeDto saved = dataVolumeService.create(request);
        return ResponseEntity.created(URI.create("/api/data-volumes/" + saved.id())).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageDataVolume(#id)")
    public ResponseEntity<DataVolumeDto> update(@PathVariable UUID id, @RequestBody @Valid UpdateDataVolumeRequest request) {
        return ResponseEntity.ok(dataVolumeService.update(id, request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageDataVolume(#id)")
    public ResponseEntity<DataVolumeDto> patch(@PathVariable UUID id, @RequestBody @Valid PatchDataVolumeRequest request) {
        return ResponseEntity.ok(dataVolumeService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageDataVolume(#id)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        dataVolumeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
