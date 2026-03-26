package com.maaz.saasPlatform.bugs.controller;

import com.maaz.saasPlatform.bugs.dto.BugVideoAccessUrlResponse;
import com.maaz.saasPlatform.bugs.dto.BugVideoMetadataResponse;
import com.maaz.saasPlatform.bugs.dto.ConfirmVideoUploadRequest;
import com.maaz.saasPlatform.bugs.dto.GenerateVideoUploadUrlRequest;
import com.maaz.saasPlatform.bugs.dto.GenerateVideoUploadUrlResponse;
import com.maaz.saasPlatform.bugs.dto.ResetBugVideoResponse;
import com.maaz.saasPlatform.bugs.service.BugVideoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/bugs/{bugId}/video")
public class BugVideoController {

    private final BugVideoService bugVideoService;

    public BugVideoController(BugVideoService bugVideoService) {
        this.bugVideoService = bugVideoService;
    }

    @PostMapping("/upload-url")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasAuthority('USER_TYPE_TESTER')")
    public ResponseEntity<GenerateVideoUploadUrlResponse> generateUploadUrl(
            @PathVariable UUID bugId,
            @Valid @RequestBody GenerateVideoUploadUrlRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(
                bugVideoService.createUploadUrl(
                        bugId,
                        resolveTenantId(httpServletRequest),
                        request
                )
        );
    }

    @PostMapping("/complete")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasAuthority('USER_TYPE_TESTER')")
    public ResponseEntity<BugVideoMetadataResponse> confirmUpload(
            @PathVariable UUID bugId,
            @Valid @RequestBody ConfirmVideoUploadRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(
                bugVideoService.confirmUpload(
                        bugId,
                        resolveTenantId(httpServletRequest),
                        request.getS3Key()
                )
        );
    }

    @GetMapping("/access-url")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasAuthority('USER_TYPE_TESTER') or hasAuthority('USER_TYPE_DEV')")
    public ResponseEntity<BugVideoAccessUrlResponse> generateAccessUrl(
            @PathVariable UUID bugId,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(
                bugVideoService.generateAccessUrl(
                        bugId,
                        resolveTenantId(httpServletRequest)
                )
        );
    }

    @DeleteMapping
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasAuthority('USER_TYPE_TESTER')")
    public ResponseEntity<ResetBugVideoResponse> resetUpload(@PathVariable UUID bugId) {
        return ResponseEntity.ok(bugVideoService.resetUpload(bugId));
    }

    private String resolveTenantId(HttpServletRequest request) {
        String tenantId = (String) request.getAttribute("tenantId");

        if (tenantId == null || tenantId.isBlank() || "public".equalsIgnoreCase(tenantId)) {
            throw new RuntimeException("Tenant-scoped upload requires a tenant token");
        }

        return tenantId;
    }
}
