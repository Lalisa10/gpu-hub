package com.trucdnd.gpu_hub_backend.cluster.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.trucdnd.gpu_hub_backend.cluster.dto.ClusterDetailsDto;
import com.trucdnd.gpu_hub_backend.cluster.dto.ClusterDto;
import com.trucdnd.gpu_hub_backend.cluster.dto.JoinClusterRequest;
import com.trucdnd.gpu_hub_backend.cluster.dto.PatchClusterRequest;
import com.trucdnd.gpu_hub_backend.cluster.service.ClusterService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clusters")
@RequiredArgsConstructor
public class ClusterController {
    private final ClusterService clusterService;
    private final Executor sseExecutor = Executors.newCachedThreadPool();

    @GetMapping
    public ResponseEntity<List<ClusterDto>> getAll() {
        return ResponseEntity.ok(clusterService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClusterDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(clusterService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ClusterDto> post(@RequestBody @Valid JoinClusterRequest request) {
        ClusterDto saved = clusterService.save(request);
        URI location = URI.create("/api/clusters/" + saved.id());
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping
    public ResponseEntity<ClusterDto> put(@RequestBody @Valid ClusterDto cluster) {
        return ResponseEntity.ok(clusterService.update(cluster));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClusterDto> putById(@PathVariable UUID id, @RequestBody @Valid JoinClusterRequest request) {
        return ResponseEntity.ok(clusterService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        clusterService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ClusterDto> patch(@PathVariable UUID id, @RequestBody @Valid PatchClusterRequest request) {
        return ResponseEntity.ok(clusterService.patch(id, request));
    }

    @PostMapping(value = "/{id}/kubeconfig", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClusterDto> uploadKubeconfig(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(clusterService.uploadKubeconfig(id, file));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<ClusterDetailsDto> getDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(clusterService.getClusterDetails(id));
    }

    @GetMapping(value = "/{id}/details/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDetails(@PathVariable UUID id) {
        SseEmitter emitter = new SseEmitter(0L);
        sseExecutor.execute(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("details")
                        .data(clusterService.getClusterDetails(id)));
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(30_000);
                    emitter.send(SseEmitter.event()
                            .name("details")
                            .data(clusterService.getClusterDetails(id)));
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
