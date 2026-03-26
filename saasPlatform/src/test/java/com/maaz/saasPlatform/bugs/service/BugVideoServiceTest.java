package com.maaz.saasPlatform.bugs.service;

import com.maaz.saasPlatform.bugs.BugSession;
import com.maaz.saasPlatform.bugs.VideoUploadStatus;
import com.maaz.saasPlatform.bugs.dto.BugVideoMetadataResponse;
import com.maaz.saasPlatform.bugs.dto.GenerateVideoUploadUrlRequest;
import com.maaz.saasPlatform.bugs.dto.GenerateVideoUploadUrlResponse;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BugVideoServiceTest {

    @Test
    void buildOriginalVideoKeyUsesTenantAwarePath() {
        UUID bugId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        assertThat(BugVideoService.buildOriginalVideoKey("tenant-acme", bugId))
                .isEqualTo("tenant-acme/11111111-1111-1111-1111-111111111111/original.mp4");
    }

    @Test
    void createUploadUrlMarksBugAsPendingAndReturnsPresignedPayload() {
        BugSessionService bugSessionService = mock(BugSessionService.class);
        BugVideoStorageService storageService = mock(BugVideoStorageService.class);
        BugVideoService service = new BugVideoService(bugSessionService, storageService);

        UUID bugId = UUID.randomUUID();
        BugSession bugSession = new BugSession();
        bugSession.setId(bugId);

        GenerateVideoUploadUrlRequest request = new GenerateVideoUploadUrlRequest();
        request.setContentType("video/mp4");
        request.setSizeBytes(1234L);

        when(bugSessionService.getBugById(bugId)).thenReturn(bugSession);
        when(storageService.buildObjectUrl("acme/" + bugId + "/original.mp4"))
                .thenReturn("https://bucket.s3.region.amazonaws.com/acme/" + bugId + "/original.mp4");
        when(storageService.generateUploadUrl("acme/" + bugId + "/original.mp4", "video/mp4"))
                .thenReturn(new GenerateVideoUploadUrlResponse(
                        "https://presigned",
                        "acme/" + bugId + "/original.mp4",
                        "https://bucket.s3.region.amazonaws.com/acme/" + bugId + "/original.mp4",
                        "video/mp4",
                        900
                ));

        GenerateVideoUploadUrlResponse response = service.createUploadUrl(bugId, "acme", request);

        assertThat(response.getS3Key()).isEqualTo("acme/" + bugId + "/original.mp4");
        assertThat(bugSession.getVideoUploadStatus()).isEqualTo(VideoUploadStatus.UPLOAD_PENDING);
        assertThat(bugSession.getVideoContentType()).isEqualTo("video/mp4");
        assertThat(bugSession.getVideoSizeBytes()).isEqualTo(1234L);
    }

    @Test
    void confirmUploadUpdatesMetadataFromS3HeadObject() {
        BugSessionService bugSessionService = mock(BugSessionService.class);
        BugVideoStorageService storageService = mock(BugVideoStorageService.class);
        BugVideoService service = new BugVideoService(bugSessionService, storageService);

        UUID bugId = UUID.randomUUID();
        String key = "acme/" + bugId + "/original.mp4";

        BugSession bugSession = new BugSession();
        bugSession.setId(bugId);

        when(bugSessionService.getBugById(bugId)).thenReturn(bugSession);
        when(storageService.verifyUploadedObject(key))
                .thenReturn(HeadObjectResponse.builder()
                        .contentType("video/mp4")
                        .contentLength(9876L)
                        .build());
        when(storageService.buildObjectUrl(key))
                .thenReturn("https://bucket.s3.region.amazonaws.com/" + key);

        BugVideoMetadataResponse response = service.confirmUpload(bugId, "acme", key);

        assertThat(response.getS3Key()).isEqualTo(key);
        assertThat(response.getContentType()).isEqualTo("video/mp4");
        assertThat(response.getSizeBytes()).isEqualTo(9876L);
        assertThat(response.getStatus()).isEqualTo("UPLOADED");
        assertThat(bugSession.getVideoUploadStatus()).isEqualTo(VideoUploadStatus.UPLOADED);
    }

    @Test
    void confirmUploadRejectsCrossTenantKey() {
        BugSessionService bugSessionService = mock(BugSessionService.class);
        BugVideoStorageService storageService = mock(BugVideoStorageService.class);
        BugVideoService service = new BugVideoService(bugSessionService, storageService);

        UUID bugId = UUID.randomUUID();
        BugSession bugSession = new BugSession();
        bugSession.setId(bugId);

        when(bugSessionService.getBugById(bugId)).thenReturn(bugSession);

        assertThrows(
                RuntimeException.class,
                () -> service.confirmUpload(bugId, "acme", "other/" + bugId + "/original.mp4")
        );
    }

    @Test
    void generateAccessUrlReturnsPresignedReadUrlForUploadedVideo() {
        BugSessionService bugSessionService = mock(BugSessionService.class);
        BugVideoStorageService storageService = mock(BugVideoStorageService.class);
        BugVideoService service = new BugVideoService(bugSessionService, storageService);

        UUID bugId = UUID.randomUUID();
        String key = "acme/" + bugId + "/original.mp4";

        BugSession bugSession = new BugSession();
        bugSession.setId(bugId);
        bugSession.setVideoS3Key(key);
        bugSession.setVideoUploadStatus(VideoUploadStatus.UPLOADED);

        when(bugSessionService.getBugById(bugId)).thenReturn(bugSession);
        when(storageService.generateDownloadUrl(key)).thenReturn("https://signed-download");
        when(storageService.getSignedUrlExpirationSeconds()).thenReturn(900L);

        assertThat(service.generateAccessUrl(bugId, "acme").getAccessUrl())
                .isEqualTo("https://signed-download");
    }
}
