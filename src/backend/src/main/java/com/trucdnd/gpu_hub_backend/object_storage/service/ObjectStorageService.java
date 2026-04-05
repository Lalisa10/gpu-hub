package com.trucdnd.gpu_hub_backend.object_storage.service;

public interface ObjectStorageService {

    String getObjectAsString(String bucketName, String objectKey);
}
