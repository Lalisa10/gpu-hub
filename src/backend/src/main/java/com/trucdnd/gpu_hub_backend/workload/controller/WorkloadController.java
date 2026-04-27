package com.trucdnd.gpu_hub_backend.workload.controller;

import com.trucdnd.gpu_hub_backend.workload.dto.CreateWorkloadRequest;
import com.trucdnd.gpu_hub_backend.workload.dto.PodInfoDto;
import com.trucdnd.gpu_hub_backend.workload.dto.WorkloadDto;
import com.trucdnd.gpu_hub_backend.workload.service.WorkloadService;
import com.trucdnd.gpu_hub_backend.workload.sse.WorkloadStreamRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/workloads")
@RequiredArgsConstructor
public class WorkloadController {
    private final WorkloadService workloadService;
    private final WorkloadStreamRegistry workloadStreamRegistry;
    private final Executor sseExecutor = Executors.newCachedThreadPool();

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

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or @rbac.canCancelWorkload(#id)")
    public ResponseEntity<WorkloadDto> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(workloadService.cancel(id));
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

    @GetMapping(value = "/{id}/status/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN') or @rbac.canAccessWorkload(#id)")
    public SseEmitter streamStatus(@PathVariable UUID id) {
        return workloadStreamRegistry.register(id);
    }

    @GetMapping(value = "/{id}/pods/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN') or @rbac.canAccessWorkload(#id)")
    public SseEmitter streamPods(@PathVariable UUID id) {
        SseEmitter emitter = new SseEmitter(0L);
        sseExecutor.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name("pods").data(workloadService.listPods(id)));
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(5_000);
                    emitter.send(SseEmitter.event().name("pods").data(workloadService.listPods(id)));
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

    @GetMapping(value = "/{id}/pods/{podName}/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN') or @rbac.canAccessWorkload(#id)")
    public SseEmitter streamPodLogs(@PathVariable UUID id, @PathVariable String podName) {
        SseEmitter emitter = new SseEmitter(0L);
        sseExecutor.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name("log").data(workloadService.getPodLogs(id, podName)));
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(5_000);
                    emitter.send(SseEmitter.event().name("log").data(workloadService.getPodLogs(id, podName)));
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
