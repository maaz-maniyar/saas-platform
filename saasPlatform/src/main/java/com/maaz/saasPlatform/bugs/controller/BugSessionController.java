package com.maaz.saasPlatform.bugs.controller;

import com.maaz.saasPlatform.bugs.BugSession;
import com.maaz.saasPlatform.bugs.dto.CreateBugRequest;
import com.maaz.saasPlatform.bugs.dto.PatchBugRequest;
import com.maaz.saasPlatform.bugs.dto.UpdateBugRequest;
import com.maaz.saasPlatform.bugs.service.BugSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bugs")
@RequiredArgsConstructor
public class BugSessionController {

    private final BugSessionService service;

    @PostMapping
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasAuthority('USER_TYPE_TESTER')")
    public ResponseEntity<BugSession> createBug(
            @Valid @RequestBody CreateBugRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();

        BugSession bug = service.createBug(
                request.getTitle(),
                request.getDescription(),
                username,
                request.getAssignedTo()
        );

        return ResponseEntity.ok(bug);
    }

    @GetMapping
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasAuthority('USER_TYPE_TESTER') or hasAuthority('USER_TYPE_DEV')")
    public ResponseEntity<List<BugSession>> getAll() {
        return ResponseEntity.ok(service.getAllBugs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasAuthority('USER_TYPE_TESTER') or hasAuthority('USER_TYPE_DEV')")
    public ResponseEntity<BugSession> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getBugById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasAuthority('USER_TYPE_TESTER')")
    public ResponseEntity<BugSession> replaceBug(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBugRequest request
    ) {
        return ResponseEntity.ok(
                service.replaceBug(
                        id,
                        request.getTitle(),
                        request.getDescription(),
                        request.getStatus(),
                        request.getAssignedTo()
                )
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasAuthority('USER_TYPE_TESTER') or hasAuthority('USER_TYPE_DEV')")
    public ResponseEntity<BugSession> patchBug(
            @PathVariable UUID id,
            @RequestBody PatchBugRequest request
    ) {
        return ResponseEntity.ok(
                service.patchBug(
                        id,
                        request.getTitle(),
                        request.getDescription(),
                        request.getStatus(),
                        request.getAssignedTo()
                )
        );
    }
}
