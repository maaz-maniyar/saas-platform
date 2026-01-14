package com.maaz.saasPlatform.auth.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET =
            "THIS_IS_A_VERY_LONG_SECRET_KEY_FOR_JWT_SIGNING_123456";

    private static final long EXPIRATION = 1000 * 60 * 60; // 1 hour

    private static final Key key =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public static String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)                // still valid
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key)                    // algorithm inferred
                .compact();
    }
}
