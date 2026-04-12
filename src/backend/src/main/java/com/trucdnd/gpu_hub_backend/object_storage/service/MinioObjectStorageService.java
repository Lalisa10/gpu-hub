package com.trucdnd.gpu_hub_backend.object_storage.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MinioObjectStorageService implements ObjectStorageService {

    private final MinioClient minioClient;

    @Override
    public String getObjectAsString(String bucketName, String objectKey) {
        try (var inputStream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(objectKey).build())) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read object from MinIO: " + objectKey, exception);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to get object from MinIO bucket '" + bucketName + "' with key '" + objectKey + "'",
                    exception);
        }
    }

    @Override
    public void putObject(String bucketName, String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, size, (long)-1)
                            .contentType(contentType)
                            .build());
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to upload object to MinIO bucket '" + bucketName + "' with key '" + objectKey + "'",
                    exception);
        }
    }
}
