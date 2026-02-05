package com.maaz.saasPlatform.security;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final Map<String, RateLimiter> tenantLimiters = new ConcurrentHashMap<>();

    public RateLimiterService(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    public boolean allowRequest(String tenantId) {
        RateLimiter limiter = tenantLimiters.computeIfAbsent(
                tenantId,
                id -> rateLimiterRegistry.rateLimiter("tenant-api")
        );

        return limiter.acquirePermission();
    }
}
