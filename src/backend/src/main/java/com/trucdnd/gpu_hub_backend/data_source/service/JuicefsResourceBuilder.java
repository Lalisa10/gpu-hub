package com.trucdnd.gpu_hub_backend.data_source.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.trucdnd.gpu_hub_backend.common.utils.RandomK8sResourceNameGenerator;
import com.trucdnd.gpu_hub_backend.data_source.config.JuicefsProperties;
import com.trucdnd.gpu_hub_backend.data_source.entity.DataSource;
import com.trucdnd.gpu_hub_backend.kubernetes.util.K8sLabels;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JuicefsResourceBuilder {

    public static final String DATA_SOURCE_ID_LABEL = K8sLabels.DATA_SOURCE_ID;
    public static final String JFS_VOLUME_NAME = "team-data";
    public static final String JFS_MOUNT_PATH = "/data";

    private final JuicefsProperties properties;
    private final RandomK8sResourceNameGenerator nameGenerator;

    public String generateJobName() {
        return "jfs-migrate-" + nameGenerator.generateString(5);
    }

    public Job buildMigrationJob(DataSource source, String namespace, String teamPvcName,
            String sourcePath, String jobName) {
        BucketEndpoint endpoint = parseBucketUrl(source.getBucketUrl());
        String trimmedPath = sourcePath == null ? "" : sourcePath.trim();
        String pathSegment = trimmedPath.isEmpty() ? "" : trimmedPath.replaceAll("^/+|/+$", "") + "/";
        String folder = source.getFolderName();
        String mcArgs = String.format(
                "mc alias set src %s %s %s && mkdir -p /data/%s && mc cp -r src/%s/%s /data/%s/",
                endpoint.endpoint(), source.getAccessKey(), source.getSecretKey(),
                folder, endpoint.bucket(), pathSegment, folder
        );

        Map<String, String> labels = buildLabels(source);

        return new JobBuilder()
                .withNewMetadata()
                    .withName(jobName)
                    .withNamespace(namespace)
                    .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                    .withBackoffLimit(properties.getBackoffLimit())
                    .withNewTemplate()
                        .withNewMetadata()
                            .withLabels(labels)
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
                                    .withClaimName(teamPvcName)
                                .endPersistentVolumeClaim()
                            .endVolume()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();
    }

    private Map<String, String> buildLabels(DataSource source) {
        Map<String, String> labels = K8sLabels.standard(
                K8sLabels.OWNER_DATA_SOURCE,
                source.getTeam().getName(),
                source.getCluster().getName(),
                source.getCreatedBy().getUsername());
        labels.put(K8sLabels.DATA_SOURCE, K8sLabels.sanitize(source.getName()));
        labels.put(K8sLabels.DATA_SOURCE_ID, source.getId().toString());
        return labels;
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

    private record BucketEndpoint(String endpoint, String bucket) {}
}
