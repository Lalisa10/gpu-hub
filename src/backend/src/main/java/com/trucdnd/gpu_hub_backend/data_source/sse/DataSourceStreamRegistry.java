package com.trucdnd.gpu_hub_backend.data_source.sse;

import com.trucdnd.gpu_hub_backend.data_source.event.DataSourceFormattedEvent;
import com.trucdnd.gpu_hub_backend.data_source.service.DataSourceService;

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
public class DataSourceStreamRegistry {

    private final ConcurrentMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final DataSourceService dataSourceService;

    public SseEmitter register(UUID dataSourceId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(dataSourceId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> remove(dataSourceId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> { emitter.complete(); cleanup.run(); });
        emitter.onError(_e -> { emitter.complete(); cleanup.run(); });

        try {
            emitter.send(SseEmitter.event().name("status").data(dataSourceService.findById(dataSourceId)));
        } catch (IOException e) {
            remove(dataSourceId, emitter);
            emitter.completeWithError(e);
        } catch (RuntimeException e) {
            remove(dataSourceId, emitter);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    private void remove(UUID dataSourceId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(dataSourceId);
        if (list == null) return;
        list.remove(emitter);
        if (list.isEmpty()) emitters.remove(dataSourceId, list);
    }

    @Async
    @EventListener
    public void onFormatted(DataSourceFormattedEvent event) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(event.dataSourceId());
        if (list == null || list.isEmpty()) return;

        Object payload;
        try {
            payload = dataSourceService.findById(event.dataSourceId());
        } catch (RuntimeException e) {
            log.warn("Failed to load data source {} for SSE push: {}", event.dataSourceId(), e.getMessage());
            return;
        }

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("status").data(payload));
            } catch (IOException | IllegalStateException e) {
                remove(event.dataSourceId(), emitter);
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
