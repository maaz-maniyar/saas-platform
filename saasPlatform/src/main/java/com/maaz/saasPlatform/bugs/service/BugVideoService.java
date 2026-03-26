package com.maaz.saasPlatform.bugs.service;

import com.maaz.saasPlatform.bugs.BugSession;
import com.maaz.saasPlatform.bugs.VideoUploadStatus;
import com.maaz.saasPlatform.bugs.dto.BugVideoAccessUrlResponse;
import com.maaz.saasPlatform.bugs.dto.BugVideoMetadataResponse;
import com.maaz.saasPlatform.bugs.dto.GenerateVideoUploadUrlRequest;
import com.maaz.saasPlatform.bugs.dto.GenerateVideoUploadUrlResponse;
import com.maaz.saasPlatform.bugs.dto.ResetBugVideoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.util.UUID;

@Service
public class BugVideoService {

    private final BugSessionService bugSessionService;
    private final BugVideoStorageService bugVideoStorageService;

    public BugVideoService(
            BugSessionService bugSessionService,
            BugVideoStorageService bugVideoStorageService
    ) {
        this.bugSessionService = bugSessionService;
        this.bugVideoStorageService = bugVideoStorageService;
    }

    @Transactional
    public GenerateVideoUploadUrlResponse createUploadUrl(
            UUID bugId,
            String tenantId,
            GenerateVideoUploadUrlRequest request
    ) {
        BugSession bugSession = bugSessionService.getBugById(bugId);
        String s3Key = buildOriginalVideoKey(tenantId, bugSession.getId());

        bugSession.setVideoS3Key(s3Key);
        bugSession.setVideoUrl(bugVideoStorageService.buildObjectUrl(s3Key));
        bugSession.setVideoContentType(request.getContentType());
        bugSession.setVideoSizeBytes(request.getSizeBytes());
        bugSession.setVideoUploadStatus(VideoUploadStatus.UPLOAD_PENDING);

        return bugVideoStorageService.generateUploadUrl(s3Key, request.getContentType());
    }

    @Transactional
    public BugVideoMetadataResponse confirmUpload(
            UUID bugId,
            String tenantId,
            String s3Key
    ) {
        BugSession bugSession = bugSessionService.getBugById(bugId);
        String expectedKey = buildOriginalVideoKey(tenantId, bugSession.getId());

        if (!expectedKey.equals(s3Key)) {
            throw new RuntimeException("Invalid S3 key for this tenant or bug");
        }

        HeadObjectResponse objectMetadata =
                bugVideoStorageService.verifyUploadedObject(expectedKey);

        bugSession.setVideoS3Key(expectedKey);
        bugSession.setVideoUrl(bugVideoStorageService.buildObjectUrl(expectedKey));
        bugSession.setVideoContentType(objectMetadata.contentType());
        bugSession.setVideoSizeBytes(objectMetadata.contentLength());
        bugSession.setVideoUploadStatus(VideoUploadStatus.UPLOADED);

        return BugVideoMetadataResponse.from(bugSession);
    }

    public static String buildOriginalVideoKey(String tenantId, UUID bugId) {
        return "%s/%s/original.mp4".formatted(tenantId, bugId);
    }

    @Transactional(readOnly = true)
    public BugVideoAccessUrlResponse generateAccessUrl(UUID bugId, String tenantId) {
        BugSession bugSession = bugSessionService.getBugById(bugId);
        String expectedKey = buildOriginalVideoKey(tenantId, bugSession.getId());

        if (bugSession.getVideoUploadStatus() != VideoUploadStatus.UPLOADED
                || bugSession.getVideoS3Key() == null
                || !expectedKey.equals(bugSession.getVideoS3Key())) {
            throw new RuntimeException("No uploaded video is available for this bug");
        }

        return new BugVideoAccessUrlResponse(
                bugVideoStorageService.generateDownloadUrl(expectedKey),
                bugVideoStorageService.getSignedUrlExpirationSeconds()
        );
    }

    @Transactional
    public ResetBugVideoResponse resetUpload(UUID bugId) {
        BugSession bugSession = bugSessionService.getBugById(bugId);
        bugSession.setVideoS3Key(null);
        bugSession.setVideoUrl(null);
        bugSession.setVideoContentType(null);
        bugSession.setVideoSizeBytes(null);
        bugSession.setVideoUploadStatus(VideoUploadStatus.NOT_UPLOADED);
        return new ResetBugVideoResponse(bugSession.getVideoUploadStatus().name());
    }
}
