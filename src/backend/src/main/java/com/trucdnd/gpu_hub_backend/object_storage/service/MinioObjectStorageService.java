package com.trucdnd.gpu_hub_backend.object_storage.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
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
}
