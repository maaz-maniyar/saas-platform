package com.maaz.saasPlatform.auth.dto;

public class LoginResponse {

    private final String accessToken;
    private final String email;
    private final String role;
    private final String userType;
    private final String tenantId;

    public LoginResponse(
            String accessToken,
            String email,
            String role,
            String userType,
            String tenantId
    ) {
        this.accessToken = accessToken;
        this.email = email;
        this.role = role;
        this.userType = userType;
        this.tenantId = tenantId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getUserType() {
        return userType;
    }

    public String getTenantId() {
        return tenantId;
    }
}
