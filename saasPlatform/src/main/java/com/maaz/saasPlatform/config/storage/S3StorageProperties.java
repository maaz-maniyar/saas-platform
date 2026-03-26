package com.maaz.saasPlatform.config.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public class S3StorageProperties {

    private String region;
    private final S3 s3 = new S3();
    private long uploadUrlExpirationSeconds = 900;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public S3 getS3() {
        return s3;
    }

    public long getUploadUrlExpirationSeconds() {
        return uploadUrlExpirationSeconds;
    }

    public void setUploadUrlExpirationSeconds(long uploadUrlExpirationSeconds) {
        this.uploadUrlExpirationSeconds = uploadUrlExpirationSeconds;
    }

    public static class S3 {
        private String bucket;

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
    }
}
