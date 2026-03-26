package com.maaz.saasPlatform.bugs.dto;

public class BugVideoAccessUrlResponse {

    private final String accessUrl;
    private final long expiresInSeconds;

    public BugVideoAccessUrlResponse(String accessUrl, long expiresInSeconds) {
        this.accessUrl = accessUrl;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }
}
