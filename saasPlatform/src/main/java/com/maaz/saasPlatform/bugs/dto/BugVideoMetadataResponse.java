package com.maaz.saasPlatform.bugs.dto;

import com.maaz.saasPlatform.bugs.BugSession;

public class BugVideoMetadataResponse {

    private final String s3Key;
    private final String url;
    private final String contentType;
    private final Long sizeBytes;
    private final String status;

    public BugVideoMetadataResponse(
            String s3Key,
            String url,
            String contentType,
            Long sizeBytes,
            String status
    ) {
        this.s3Key = s3Key;
        this.url = url;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.status = status;
    }

    public static BugVideoMetadataResponse from(BugSession bugSession) {
        return new BugVideoMetadataResponse(
                bugSession.getVideoS3Key(),
                bugSession.getVideoUrl(),
                bugSession.getVideoContentType(),
                bugSession.getVideoSizeBytes(),
                bugSession.getVideoUploadStatus().name()
        );
    }

    public String getS3Key() {
        return s3Key;
    }

    public String getUrl() {
        return url;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getStatus() {
        return status;
    }
}
