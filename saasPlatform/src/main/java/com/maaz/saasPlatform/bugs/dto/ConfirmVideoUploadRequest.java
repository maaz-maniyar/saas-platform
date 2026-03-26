package com.maaz.saasPlatform.bugs.dto;

import jakarta.validation.constraints.NotBlank;

public class ConfirmVideoUploadRequest {

    @NotBlank
    private String s3Key;

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }
}
