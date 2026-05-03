package com.trucdnd.gpu_hub_backend.data_source.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.trucdnd.gpu_hub_backend.cluster.entity.Cluster;
import com.trucdnd.gpu_hub_backend.common.utils.RandomK8sResourceNameGenerator;
import com.trucdnd.gpu_hub_backend.data_source.config.JuicefsProperties;
import com.trucdnd.gpu_hub_backend.data_source.entity.DataSource;

import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.PersistentVolumeBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JuicefsResourceBuilder {

    public static final String DATA_SOURCE_ID_LABEL = "gpu-hub/data-source-id";
    public static final String OWNER_LABEL = "gpu-hub/owner";
    public static final String OWNER_VALUE = "data-source";
    public static final String JFS_NAME_LABEL = "juicefs-name";
    public static final String JFS_VOLUME_NAME = "jfs-volume";
    public static final String JFS_MOUNT_PATH = "/data";

    private final JuicefsProperties properties;
    private final RandomK8sResourceNameGenerator nameGenerator;

    public Names generateNames() {
        String suffix = nameGenerator.generateString(5);
        return new Names(
                "jfs-secret-" + suffix,
                "jfs-pv-" + suffix,
                "jfs-migrate-" + suffix,
                "jfs-" + suffix
        );
    }

    public Secret buildSecret(DataSource source, Cluster cluster, String namespace, Names names) {
        return new SecretBuilder()
                .withNewMetadata()
                    .withName(names.secretName())
                    .withNamespace(namespace)
                    .withLabels(commonLabels(source))
                .endMetadata()
                .withType("Opaque")
                .addToStringData("name", names.fsName())
                .addToStringData("metaurl", cluster.getJuicefsMetaurl())
                .addToStringData("storage", properties.getStorageType())
                .addToStringData("bucket", source.getBucketUrl())
                .addToStringData("access-key", source.getAccessKey())
                .addToStringData("secret-key", source.getSecretKey())
                .build();
    }

    public PersistentVolume buildPersistentVolume(DataSource source, String namespace, Names names) {
        Map<String, String> labels = commonLabels(source);
        labels.put(JFS_NAME_LABEL, names.fsName());

        return new PersistentVolumeBuilder()
                .withNewMetadata()
                    .withName(names.pvName())
                    .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                    .withCapacity(Map.of("storage", new Quantity(properties.getPvCapacity())))
                    .withVolumeMode("Filesystem")
                    .withAccessModes("ReadWriteMany")
                    .withPersistentVolumeReclaimPolicy("Retain")
                    .withNewCsi()
                        .withDriver("csi.juicefs.com")
                        .withVolumeHandle(names.pvName())
                        .withFsType("juicefs")
                        .withNewNodePublishSecretRef()
                            .withName(names.secretName())
                            .withNamespace(namespace)
                        .endNodePublishSecretRef()
                    .endCsi()
                .endSpec()
                .build();
    }

    public PersistentVolumeClaim buildPersistentVolumeClaim(DataSource source, String namespace,
            String pvcName, Names names) {
        return new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                    .withName(pvcName)
                    .withNamespace(namespace)
                    .withLabels(commonLabels(source))
                .endMetadata()
                .withNewSpec()
                    .withAccessModes("ReadWriteMany")
                    .withVolumeMode("Filesystem")
                    .withStorageClassName("")
                    .withNewResources()
                        .addToRequests("storage", new Quantity(properties.getPvCapacity()))
                    .endResources()
                    .withNewSelector()
                        .addToMatchLabels(JFS_NAME_LABEL, names.fsName())
                    .endSelector()
                .endSpec()
                .build();
    }

    public Job buildMigrationJob(DataSource source, String namespace, String pvcName,
            String sourcePath, Names names) {
        BucketEndpoint endpoint = parseBucketUrl(source.getBucketUrl());
        String trimmedPath = sourcePath == null ? "" : sourcePath.trim();
        String pathSegment = trimmedPath.isEmpty() ? "" : trimmedPath.replaceAll("^/+|/+$", "") + "/";
        String mcArgs = String.format(
                "mc alias set src %s %s %s && mc cp -r src/%s/%s /data/",
                endpoint.endpoint(), source.getAccessKey(), source.getSecretKey(),
                endpoint.bucket(), pathSegment
        );

        return new JobBuilder()
                .withNewMetadata()
                    .withName(names.jobName())
                    .withNamespace(namespace)
                    .withLabels(commonLabels(source))
                .endMetadata()
                .withNewSpec()
                    .withBackoffLimit(properties.getBackoffLimit())
                    .withNewTemplate()
                        .withNewMetadata()
                            .withLabels(commonLabels(source))
                        .endMetadata()
                        .withNewSpec()
                            .withRestartPolicy("OnFailure")
                            .addNewContainer()
                                .withName("migrator")
                                .withImage(properties.getMigrationImage())
                                .withCommand("/bin/sh", "-c")
                                .withArgs(mcArgs)
                                .addNewVolumeMount()
                                    .withName(JFS_VOLUME_NAME)
                                    .withMountPath(JFS_MOUNT_PATH)
                                .endVolumeMount()
                            .endContainer()
                            .addNewVolume()
                                .withName(JFS_VOLUME_NAME)
                                .withNewPersistentVolumeClaim()
                                    .withClaimName(pvcName)
                                .endPersistentVolumeClaim()
                            .endVolume()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();
    }

    private Map<String, String> commonLabels(DataSource source) {
        return new java.util.HashMap<>(Map.of(
                DATA_SOURCE_ID_LABEL, source.getId().toString(),
                OWNER_LABEL, OWNER_VALUE
        ));
    }

    private BucketEndpoint parseBucketUrl(String bucketUrl) {
        try {
            URI uri = new URI(bucketUrl);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath() == null ? "" : uri.getPath();
            String bucket = path.startsWith("/") ? path.substring(1) : path;
            String endpoint = port == -1
                    ? scheme + "://" + host
                    : scheme + "://" + host + ":" + port;
            return new BucketEndpoint(endpoint, bucket);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid bucket_url: " + bucketUrl, e);
        }
    }

    public record Names(String secretName, String pvName, String jobName, String fsName) {}

    private record BucketEndpoint(String endpoint, String bucket) {}
}
