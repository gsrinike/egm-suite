package com.infra.storage.object.minio;

import com.infra.storage.object.ObjectStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Locale;

/**
 * MinIO implementation of the generic object-storage boundary.
 *
 * <p>Buckets are initialized at application startup. Request-time storage only
 * writes the object payload so concurrent uploads do not race on bucket
 * creation.</p>
 */
public class MinioObjectStorageService implements ObjectStorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinioObjectStorageService.class);
    private static final int INITIALIZE_BUCKET_ATTEMPTS = 10;
    private static final long INITIALIZE_BUCKET_RETRY_DELAY_MILLIS = 1_000L;

    private final MinioClient minioClient;

    public MinioObjectStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public void initializeBucket(String bucketName) {
        for (int attempt = 1; attempt <= INITIALIZE_BUCKET_ATTEMPTS; attempt++) {
            try {
                initializeBucketOnce(bucketName);
                return;
            } catch (Exception exception) {
                if (attempt == INITIALIZE_BUCKET_ATTEMPTS) {
                    throw new IllegalStateException("Unable to initialize object-storage bucket " + bucketName, exception);
                }
                LOGGER.warn("Unable to initialize object-storage bucket {} on attempt {}/{}; retrying",
                        bucketName, attempt, INITIALIZE_BUCKET_ATTEMPTS);
                sleepBeforeRetry();
            }
        }
    }

    private void initializeBucketOnce(String bucketName) throws Exception {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!bucketExists) {
            try {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                LOGGER.info("Created object-storage bucket {}", bucketName);
            } catch (ErrorResponseException exception) {
                if (!isBucketAlreadyAvailable(exception)) {
                    throw exception;
                }
                LOGGER.info("Object-storage bucket {} already exists", bucketName);
            }
        } else {
            LOGGER.info("Object-storage bucket {} already exists", bucketName);
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(INITIALIZE_BUCKET_RETRY_DELAY_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while initializing object-storage bucket", exception);
        }
    }

    @Override
    public void store(String bucketName, String objectName, byte[] bytes, String contentType) {
        try {
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

    private boolean isBucketAlreadyAvailable(ErrorResponseException exception) {
        String code = exception.errorResponse() == null ? null : exception.errorResponse().code();
        if ("BucketAlreadyOwnedByYou".equals(code) || "BucketAlreadyExists".equals(code)) {
            return true;
        }
        String message = exception.getMessage() == null ? exception.toString() : exception.getMessage();
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        return normalizedMessage.contains("bucketalreadyownedbyyou")
                || normalizedMessage.contains("bucketalreadyexists")
                || normalizedMessage.contains("you already own it")
                || normalizedMessage.contains("named bucket succeeded");
    }

    @Override
    public byte[] read(String bucketName, String objectName) {
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build())) {
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read object payload", exception);
        }
    }
}
