package com.trucdnd.gpu_hub_backend.object_storage.service;

import java.io.InputStream;

public interface ObjectStorageService {

    String getObjectAsString(String bucketName, String objectKey);

    void putObject(String bucketName, String objectKey, InputStream inputStream, long size, String contentType);
}