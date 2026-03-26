package com.maaz.saasPlatform.bugs.dto;

public class GenerateVideoUploadUrlResponse {

    private final String uploadUrl;
    private final String s3Key;
    private final String objectUrl;
    private final String contentType;
    private final long expiresInSeconds;

    public GenerateVideoUploadUrlResponse(
            String uploadUrl,
            String s3Key,
            String objectUrl,
            String contentType,
            long expiresInSeconds
    ) {
        this.uploadUrl = uploadUrl;
        this.s3Key = s3Key;
        this.objectUrl = objectUrl;
        this.contentType = contentType;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public String getS3Key() {
        return s3Key;
    }

    public String getObjectUrl() {
        return objectUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }
}
