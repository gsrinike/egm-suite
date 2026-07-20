package com.infra.storage.object;

/**
 * Fallback object-storage adapter used by services that do not configure object storage.
 */
public class DisabledObjectStorageService implements ObjectStorageService {
    @Override
    public void initializeBucket(String bucketName) {
        throw disabled();
    }

    @Override
    public void store(String bucketName, String objectName, byte[] bytes, String contentType) {
        throw disabled();
    }

    @Override
    public byte[] read(String bucketName, String objectName) {
        throw disabled();
    }

    private IllegalStateException disabled() {
        return new IllegalStateException("Object storage is not configured for this module");
    }
}
