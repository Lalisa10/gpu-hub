package com.trucdnd.gpu_hub_backend.data_source.service;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.common.constants.DataSource.Status;
import com.trucdnd.gpu_hub_backend.data_source.entity.DataSource;
import com.trucdnd.gpu_hub_backend.data_source.event.DataSourceFormattedEvent;
import com.trucdnd.gpu_hub_backend.data_source.repository.DataSourceRepository;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.Watcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSourceJobReconciler {

    private final DataSourceRepository dataSourceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void onJobEvent(Cluster cluster, Watcher.Action action, Job job) {
        String label = job.getMetadata() != null && job.getMetadata().getLabels() != null
                ? job.getMetadata().getLabels().get(JuicefsResourceBuilder.DATA_SOURCE_ID_LABEL)
                : null;
        if (label == null) return;

        UUID dataSourceId;
        try {
            dataSourceId = UUID.fromString(label);
        } catch (IllegalArgumentException e) {
            log.debug("Skipping job with non-UUID data-source-id label: {}", label);
            return;
        }

        DataSource source = dataSourceRepository.findById(dataSourceId).orElse(null);
        if (source == null) {
            log.debug("Job event for unknown data_source {} (action={})", dataSourceId, action);
            return;
        }
        if (source.getStatus() == Status.FORMATED) return;

        Integer succeeded = job.getStatus() != null ? job.getStatus().getSucceeded() : null;
        Integer failed = job.getStatus() != null ? job.getStatus().getFailed() : null;

        if (succeeded != null && succeeded >= 1) {
            source.setStatus(Status.FORMATED);
            dataSourceRepository.save(source);
            eventPublisher.publishEvent(new DataSourceFormattedEvent(source.getId()));
            log.info("DataSource {} marked formated (job {} succeeded)", source.getId(),
                    job.getMetadata().getName());
        } else if (failed != null && failed >= 1) {
            log.warn("Migration job {} for data_source {} reports {} failed pod(s); status stays FORMATING",
                    job.getMetadata().getName(), source.getId(), failed);
        }
    }
}
