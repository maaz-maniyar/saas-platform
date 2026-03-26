package com.maaz.saasPlatform.user.dto;

import com.maaz.saasPlatform.user.model.User;

public class UserResponse {

    private final Long id;
    private final String email;
    private final String role;
    private final String userType;

    public UserResponse(Long id, String email, String role, String userType) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.userType = userType;
    }

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getUserType() != null ? user.getUserType().name() : null
        );
    }

    public Long getId() {
        return id;
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
}
