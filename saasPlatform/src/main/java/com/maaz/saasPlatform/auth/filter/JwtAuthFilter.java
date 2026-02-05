package com.maaz.saasPlatform.auth.filter;

import com.maaz.saasPlatform.security.RateLimiterService;
import com.maaz.saasPlatform.tenant.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    public JwtAuthFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String tenantId = TenantContext.getTenant();
        if (tenantId != null && !rateLimiterService.allowRequest(tenantId)) {
            response.setStatus(429);
            response.getWriter().write("Rate limit exceeded for tenant");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
