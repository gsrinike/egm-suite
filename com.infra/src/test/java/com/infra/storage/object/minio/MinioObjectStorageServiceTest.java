package com.infra.storage.object.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioObjectStorageServiceTest {

    @Test
    void treatsAlreadyOwnedBucketAsSuccessfulInitialization() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        doThrow(error("BucketAlreadyOwnedByYou")).when(minioClient).makeBucket(any(MakeBucketArgs.class));
        MinioObjectStorageService service = new MinioObjectStorageService(minioClient);

        service.initializeBucket("raw-files");

        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
    }

    @Test
    void treatsAlreadyOwnedBucketMessageAsSuccessfulInitialization() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        doThrow(error(null, "Your previous request to create the named bucket succeeded and you already own it."))
                .when(minioClient)
                .makeBucket(any(MakeBucketArgs.class));
        MinioObjectStorageService service = new MinioObjectStorageService(minioClient);

        service.initializeBucket("raw-files");

        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
    }

    @Test
    void storesObjectWithoutCheckingOrCreatingBucket() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        MinioObjectStorageService service = new MinioObjectStorageService(minioClient);

        service.store("raw-files", "network/file.xml", "payload".getBytes(), "application/octet-stream");

        verify(minioClient, never()).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    private ErrorResponseException error(String code) {
        return error(code, "already exists");
    }

    private ErrorResponseException error(String code, String message) {
        ErrorResponse errorResponse = new ErrorResponse(code, message, "raw-files", null, "raw-files", "request-id", "host-id");
        return new ErrorResponseException(errorResponse, null, null);
    }
}
