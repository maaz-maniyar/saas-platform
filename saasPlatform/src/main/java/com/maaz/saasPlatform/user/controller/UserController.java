package com.maaz.saasPlatform.user.controller;

import com.maaz.saasPlatform.user.dto.CreateUserRequest;
import com.maaz.saasPlatform.user.dto.UserResponse;
import com.maaz.saasPlatform.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/platform/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<UserResponse> getPlatformUsers() {
        return userService.getPlatformUsers()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @PostMapping("/platform/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public UserResponse createPlatformUser(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.createPlatformUser(request));
    }

    @GetMapping("/platform/tenants/{tenantId}/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<UserResponse> getTenantUsers(@PathVariable String tenantId) {
        return userService.getUsersForTenant(tenantId)
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @PostMapping("/platform/tenants/{tenantId}/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public UserResponse createTenantUser(
            @PathVariable String tenantId,
            @Valid @RequestBody CreateUserRequest request
    ) {
        return UserResponse.from(userService.createUserForTenant(tenantId, request));
    }

    @GetMapping("/tenant/users")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public List<UserResponse> getCurrentTenantUsers() {
        return userService.getCurrentTenantUsers()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @PostMapping("/tenant/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public UserResponse createCurrentTenantUser(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.createCurrentTenantUser(request));
    }
}
