package com.trucdnd.gpu_hub_backend.data_source.service;

import org.springframework.stereotype.Component;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.data_source.entity.DataSource;
import com.trucdnd.gpu_hub_backend.kubernetes.service.BuiltinResourceService;

import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSourceProvisioner {

    private final BuiltinResourceService builtinResourceService;
    private final JuicefsResourceBuilder builder;

    public void provision(DataSource source, String namespace, String pvcName, String sourcePath) {
        Cluster cluster = source.getVolume().getCluster();
        JuicefsResourceBuilder.Names names = builder.generateNames();

        try {
            Secret secret = builder.buildSecret(source, cluster, namespace, names);
            builtinResourceService.createSecret(cluster, namespace, secret);

            PersistentVolume pv = builder.buildPersistentVolume(source, namespace, names);
            builtinResourceService.createPersistentVolume(cluster, pv);

            PersistentVolumeClaim pvc = builder.buildPersistentVolumeClaim(source, namespace, pvcName, names);
            builtinResourceService.createPersistentVolumeClaim(cluster, namespace, pvc);

            Job job = builder.buildMigrationJob(source, namespace, pvcName, sourcePath, names);
            builtinResourceService.createJob(cluster, namespace, job);

            log.info("Provisioned juicefs resources for data_source {} in namespace {}", source.getId(), namespace);
        } catch (RuntimeException e) {
            log.error("Failed to provision juicefs resources for data_source {} ({}): {} — attempting label-keyed teardown",
                    source.getId(), namespace, e.getMessage());
            teardown(cluster, namespace, source.getId().toString());
            throw e;
        }
    }

    public void teardown(Cluster cluster, String namespace, String dataSourceId) {
        String key = JuicefsResourceBuilder.DATA_SOURCE_ID_LABEL;
        try {
            builtinResourceService.deleteJobsByLabel(cluster, namespace, key, dataSourceId);
        } catch (RuntimeException e) {
            log.warn("Job teardown failed for data_source {}: {}", dataSourceId, e.getMessage());
        }
        try {
            builtinResourceService.deletePersistentVolumeClaimsByLabel(cluster, namespace, key, dataSourceId);
        } catch (RuntimeException e) {
            log.warn("PVC teardown failed for data_source {}: {}", dataSourceId, e.getMessage());
        }
        try {
            builtinResourceService.deletePersistentVolumesByLabel(cluster, key, dataSourceId);
        } catch (RuntimeException e) {
            log.warn("PV teardown failed for data_source {}: {}", dataSourceId, e.getMessage());
        }
        try {
            builtinResourceService.deleteSecretsByLabel(cluster, namespace, key, dataSourceId);
        } catch (RuntimeException e) {
            log.warn("Secret teardown failed for data_source {}: {}", dataSourceId, e.getMessage());
        }
    }
}
