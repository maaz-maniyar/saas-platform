package com.maaz.saasPlatform.tenant.filter;

import com.maaz.saasPlatform.tenant.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestUri = request.getRequestURI();

        if (requestUri.startsWith("/platform")
                || requestUri.startsWith("/auth")
                || requestUri.startsWith("/health")) {
            TenantContext.clear();
            filterChain.doFilter(request, response);
            return;
        }

        String tenantId = request.getHeader(TENANT_HEADER);
        String tokenTenantId = (String) request.getAttribute("tenantId");

        if (tokenTenantId != null && !tokenTenantId.isBlank()) {
            if (tenantId != null
                    && !tenantId.isBlank()
                    && !tokenTenantId.equalsIgnoreCase(tenantId)) {
                response.sendError(HttpStatus.FORBIDDEN.value(), "Tenant mismatch");
                return;
            }

            TenantContext.setTenant(resolveSchema(tokenTenantId));
        } else if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.setTenant("tenant_" + tenantId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveSchema(String tenantId) {
        if ("public".equalsIgnoreCase(tenantId)) {
            return "public";
        }
        return "tenant_" + tenantId;
    }
}
