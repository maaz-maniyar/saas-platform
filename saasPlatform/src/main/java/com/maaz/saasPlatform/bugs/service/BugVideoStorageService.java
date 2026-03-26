package com.maaz.saasPlatform.bugs.service;

import com.maaz.saasPlatform.bugs.dto.GenerateVideoUploadUrlResponse;
import com.maaz.saasPlatform.config.storage.S3StorageProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Duration;

@Service
public class BugVideoStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3StorageProperties properties;

    public BugVideoStorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            S3StorageProperties properties
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    public GenerateVideoUploadUrlResponse generateUploadUrl(
            String s3Key,
            String contentType
    ) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getS3().getBucket())
                .key(s3Key)
                .contentType(contentType)
                .build();

        long expiresInSeconds = properties.getUploadUrlExpirationSeconds();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expiresInSeconds))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        return new GenerateVideoUploadUrlResponse(
                presigned.url().toString(),
                s3Key,
                buildObjectUrl(s3Key),
                contentType,
                expiresInSeconds
        );
    }

    public HeadObjectResponse verifyUploadedObject(String s3Key) {
        return s3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(properties.getS3().getBucket())
                        .key(s3Key)
                        .build()
                );
    }

    public String generateDownloadUrl(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getS3().getBucket())
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(properties.getUploadUrlExpirationSeconds()))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
    }

    public long getSignedUrlExpirationSeconds() {
        return properties.getUploadUrlExpirationSeconds();
    }

    public String buildObjectUrl(String s3Key) {
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                properties.getS3().getBucket(),
                properties.getRegion(),
                s3Key
        );
    }
}
