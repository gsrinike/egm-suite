package com.infra.minio;

import com.infra.storage.ObjectStorageService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

/**
 * MinIO implementation of the generic object-storage boundary.
 */
public class MinioObjectStorageService implements ObjectStorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinioObjectStorageService.class);

    private final MinioClient minioClient;

    public MinioObjectStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public void store(String bucketName, String objectName, byte[] bytes, String contentType) {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                // Creating buckets lazily keeps local and docker-compose environments easy to bootstrap.
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(contentType)
                    .build());
            LOGGER.info("Stored object {}/{}", bucketName, objectName);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to store object payload", exception);
        }
    }
}
