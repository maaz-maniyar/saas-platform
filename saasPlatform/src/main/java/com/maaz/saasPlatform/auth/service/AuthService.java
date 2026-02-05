package com.maaz.saasPlatform.auth.service;

import com.maaz.saasPlatform.auth.util.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtUtil jwtUtil;

    public AuthService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public String login(String email, String password) {

        if (!email.endsWith("@admin.com")) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtUtil.generateToken(
                email,
                "platform_admin",
                "PLATFORM_ADMIN"
        );
    }
}
