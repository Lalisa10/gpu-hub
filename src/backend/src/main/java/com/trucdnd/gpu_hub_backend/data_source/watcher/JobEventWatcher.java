package com.trucdnd.gpu_hub_backend.data_source.watcher;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.cluster.repository.ClusterRepository;
import com.trucdnd.gpu_hub_backend.data_source.service.DataSourceJobReconciler;
import com.trucdnd.gpu_hub_backend.data_source.service.JuicefsResourceBuilder;
import com.trucdnd.gpu_hub_backend.kubernetes.factory.KubernetesClientFactory;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobEventWatcher {

    private static final long INITIAL_BACKOFF_SECONDS = 5;
    private static final long MAX_BACKOFF_SECONDS = 30;

    private final ClusterRepository clusterRepository;
    private final KubernetesClientFactory clientFactory;
    private final DataSourceJobReconciler reconciler;

    private final ConcurrentMap<UUID, Watch> watches = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> backoffSeconds = new ConcurrentHashMap<>();
    private final ScheduledExecutorService restartScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "job-watcher-restart");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void startAll() {
        for (Cluster cluster : clusterRepository.findAll()) {
            try {
                startWatch(cluster);
            } catch (RuntimeException e) {
                log.warn("Failed to start job watch for cluster {}: {}", cluster.getId(), e.getMessage());
                scheduleRestart(cluster);
            }
        }
    }

    @PreDestroy
    public void stopAll() {
        watches.values().forEach(w -> {
            try { w.close(); } catch (Exception ignored) { /* best effort */ }
        });
        watches.clear();
        restartScheduler.shutdownNow();
    }

    @Scheduled(fixedDelay = 60_000)
    public void reapMissingWatches() {
        for (Cluster cluster : clusterRepository.findAll()) {
            if (!watches.containsKey(cluster.getId())) {
                try {
                    startWatch(cluster);
                } catch (RuntimeException e) {
                    log.debug("Reaper retry failed for cluster {}: {}", cluster.getId(), e.getMessage());
                }
            }
        }
    }

    private void startWatch(Cluster cluster) {
        KubernetesClient client = clientFactory.createClient(cluster);
        Watcher<Job> watcher = new Watcher<>() {
            @Override
            public void eventReceived(Action action, Job job) {
                try {
                    reconciler.onJobEvent(cluster, action, job);
                } catch (RuntimeException e) {
                    log.warn("Reconciler error for job event on cluster {}: {}",
                            cluster.getId(), e.getMessage());
                }
            }

            @Override
            public void onClose(WatcherException cause) {
                log.warn("Job watch closed for cluster {}: {}", cluster.getId(),
                        cause != null ? cause.getMessage() : "(no cause)");
                watches.remove(cluster.getId());
                scheduleRestart(cluster);
            }
        };

        Watch watch = client.batch().v1().jobs()
                .inAnyNamespace()
                .withLabel(JuicefsResourceBuilder.DATA_SOURCE_ID_LABEL)
                .watch(watcher);
        watches.put(cluster.getId(), watch);
        backoffSeconds.put(cluster.getId(), INITIAL_BACKOFF_SECONDS);
        log.info("Job watch started for cluster {}", cluster.getId());

        try {
            List<Job> jobs = client.batch().v1().jobs()
                    .inAnyNamespace()
                    .withLabel(JuicefsResourceBuilder.DATA_SOURCE_ID_LABEL)
                    .list().getItems();
            for (Job job : jobs) {
                try {
                    reconciler.onJobEvent(cluster, Watcher.Action.MODIFIED, job);
                } catch (RuntimeException e) {
                    log.debug("Reconciler error during initial sync on cluster {}: {}",
                            cluster.getId(), e.getMessage());
                }
            }
        } catch (RuntimeException e) {
            log.debug("Initial job list failed on cluster {}: {}", cluster.getId(), e.getMessage());
        }
    }

    private void scheduleRestart(Cluster cluster) {
        long delay = backoffSeconds.getOrDefault(cluster.getId(), INITIAL_BACKOFF_SECONDS);
        backoffSeconds.put(cluster.getId(), Math.min(delay * 2, MAX_BACKOFF_SECONDS));
        restartScheduler.schedule(() -> {
            try {
                startWatch(cluster);
            } catch (RuntimeException e) {
                log.debug("Restart attempt failed for cluster {}: {}", cluster.getId(), e.getMessage());
                scheduleRestart(cluster);
            }
        }, delay, TimeUnit.SECONDS);
    }
}
