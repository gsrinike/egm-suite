package com.infra.storage.object;

/**
 * Generic object-storage boundary for storing binary payloads such as uploads,
 * reports, or generated artifacts.
 */
public interface ObjectStorageService {
    void initializeBucket(String bucketName);

    void store(String bucketName, String objectName, byte[] bytes, String contentType);

    byte[] read(String bucketName, String objectName);
}
