package com.trucdnd.gpu_hub_backend.data_source.controller;

import com.trucdnd.gpu_hub_backend.common.constants.User.GlobalRole;
import com.trucdnd.gpu_hub_backend.common.security.UserPrincipal;
import com.trucdnd.gpu_hub_backend.data_source.dto.CreateDataSourceRequest;
import com.trucdnd.gpu_hub_backend.data_source.dto.DataSourceDto;
import com.trucdnd.gpu_hub_backend.data_source.dto.PatchDataSourceRequest;
import com.trucdnd.gpu_hub_backend.data_source.dto.UpdateDataSourceRequest;
import com.trucdnd.gpu_hub_backend.data_source.service.DataSourceService;
import com.trucdnd.gpu_hub_backend.data_source.sse.DataSourceStreamRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/data-sources")
@RequiredArgsConstructor
public class DataSourceController {
    private final DataSourceService dataSourceService;
    private final DataSourceStreamRegistry dataSourceStreamRegistry;
    private final Executor sseExecutor = Executors.newCachedThreadPool();

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DataSourceDto>> getAll(@RequestParam(required = false) UUID volumeId) {
        if (volumeId != null) {
            return ResponseEntity.ok(dataSourceService.findByVolume(volumeId));
        }
        return ResponseEntity.ok(dataSourceService.findAll());
    }

    @GetMapping("/my")
    public ResponseEntity<List<DataSourceDto>> getMine(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getGlobalRole() == GlobalRole.ADMIN) {
            return ResponseEntity.ok(dataSourceService.findAll());
        }
        return ResponseEntity.ok(dataSourceService.findByUser(principal.getUserId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageDataSource(#id)")
    public ResponseEntity<DataSourceDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(dataSourceService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageTeam(#request.teamId())")
    public ResponseEntity<DataSourceDto> create(@RequestBody @Valid CreateDataSourceRequest request) {
        DataSourceDto saved = dataSourceService.create(request);
        return ResponseEntity.created(URI.create("/api/data-sources/" + saved.id())).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageDataSource(#id)")
    public ResponseEntity<DataSourceDto> update(@PathVariable UUID id, @RequestBody @Valid UpdateDataSourceRequest request) {
        return ResponseEntity.ok(dataSourceService.update(id, request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageDataSource(#id)")
    public ResponseEntity<DataSourceDto> patch(@PathVariable UUID id, @RequestBody @Valid PatchDataSourceRequest request) {
        return ResponseEntity.ok(dataSourceService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageDataSource(#id)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        dataSourceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/status/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageDataSource(#id)")
    public SseEmitter streamStatus(@PathVariable UUID id) {
        return dataSourceStreamRegistry.register(id);
    }

    @GetMapping(value = "/{id}/job-logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN') or @rbac.canManageDataSource(#id)")
    public SseEmitter streamJobLogs(@PathVariable UUID id) {
        SseEmitter emitter = new SseEmitter(0L);
        sseExecutor.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name("log").data(dataSourceService.getMigrationJobLogs(id)));
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(5_000);
                    emitter.send(SseEmitter.event().name("log").data(dataSourceService.getMigrationJobLogs(id)));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        emitter.onTimeout(emitter::complete);
        emitter.onError(_e -> emitter.complete());
        return emitter;
    }
}
