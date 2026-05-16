package com.trucdnd.gpu_hub_backend.data_source.service;

import org.springframework.stereotype.Component;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.data_source.entity.DataSource;
import com.trucdnd.gpu_hub_backend.kubernetes.service.BuiltinResourceService;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSourceProvisioner {

    private final BuiltinResourceService builtinResourceService;
    private final JuicefsResourceBuilder builder;

    public void provision(DataSource source, String namespace, String teamPvcName, String sourcePath) {
        Cluster cluster = source.getCluster();
        String jobName = builder.generateJobName();

        try {
            Job job = builder.buildMigrationJob(source, namespace, teamPvcName, sourcePath, jobName);
            builtinResourceService.createJob(cluster, namespace, job);
            log.info("Provisioned migration job for data_source {} in namespace {}", source.getId(), namespace);
        } catch (RuntimeException e) {
            log.error("Failed to provision migration job for data_source {} ({}): {} — attempting label-keyed teardown",
                    source.getId(), namespace, e.getMessage());
            teardown(cluster, namespace, source.getId().toString());
            throw e;
        }
    }

    public void teardown(Cluster cluster, String namespace, String dataSourceId) {
        try {
            builtinResourceService.deleteJobsByLabel(
                    cluster, namespace, JuicefsResourceBuilder.DATA_SOURCE_ID_LABEL, dataSourceId);
        } catch (RuntimeException e) {
            log.warn("Job teardown failed for data_source {}: {}", dataSourceId, e.getMessage());
        }
    }
}
