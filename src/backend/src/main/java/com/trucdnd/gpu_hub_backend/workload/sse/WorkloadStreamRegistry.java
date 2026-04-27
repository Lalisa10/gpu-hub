package com.trucdnd.gpu_hub_backend.workload.sse;

import com.trucdnd.gpu_hub_backend.workload.event.WorkloadStatusChangedEvent;
import com.trucdnd.gpu_hub_backend.workload.service.WorkloadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkloadStreamRegistry {

    private final ConcurrentMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final WorkloadService workloadService;

    public SseEmitter register(UUID workloadId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(workloadId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> remove(workloadId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> { emitter.complete(); cleanup.run(); });
        emitter.onError(_e -> { emitter.complete(); cleanup.run(); });

        try {
            emitter.send(SseEmitter.event().name("status").data(workloadService.findById(workloadId)));
        } catch (IOException e) {
            remove(workloadId, emitter);
            emitter.completeWithError(e);
        } catch (RuntimeException e) {
            remove(workloadId, emitter);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    private void remove(UUID workloadId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(workloadId);
        if (list == null) return;
        list.remove(emitter);
        if (list.isEmpty()) emitters.remove(workloadId, list);
    }

    @Async
    @EventListener
    public void onStatusChanged(WorkloadStatusChangedEvent event) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(event.workloadId());
        if (list == null || list.isEmpty()) return;

        Object payload;
        try {
            payload = workloadService.findById(event.workloadId());
        } catch (RuntimeException e) {
            log.warn("Failed to load workload {} for SSE push: {}", event.workloadId(), e.getMessage());
            return;
        }

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("status").data(payload));
            } catch (IOException | IllegalStateException e) {
                remove(event.workloadId(), emitter);
                emitter.completeWithError(e);
            }
        }
    }

    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        for (var entry : emitters.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().comment("keepalive"));
                } catch (IOException | IllegalStateException e) {
                    remove(entry.getKey(), emitter);
                }
            }
        }
    }
}
