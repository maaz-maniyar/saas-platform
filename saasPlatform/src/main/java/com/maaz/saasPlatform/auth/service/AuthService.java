package com.maaz.saasPlatform.auth.service;

import com.maaz.saasPlatform.auth.dto.LoginRequest;
import com.maaz.saasPlatform.auth.dto.LoginResponse;
import com.maaz.saasPlatform.auth.model.Role;
import com.maaz.saasPlatform.auth.util.JwtUtil;
import com.maaz.saasPlatform.tenant.context.TenantContext;
import com.maaz.saasPlatform.tenant.repository.TenantRepository;
import com.maaz.saasPlatform.user.model.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final TenantRepository tenantRepository;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthService(
            JwtUtil jwtUtil,
            AuthenticationManager authenticationManager,
            TenantRepository tenantRepository,
            CustomUserDetailsService customUserDetailsService
    ) {
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.tenantRepository = tenantRepository;
        this.customUserDetailsService = customUserDetailsService;
    }

    public LoginResponse login(LoginRequest request) {
        String tenantId = normalizeTenantId(request.getTenantId());
        String schema = resolveSchema(tenantId);

        TenantContext.setTenant(schema);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            User user = customUserDetailsService.loadDomainUserByEmail(request.getEmail());
            validateTenantRoleCombination(user, tenantId);

            String token = jwtUtil.generateToken(
                    user.getEmail(),
                    tenantId,
                    user.getRole().name(),
                    user.getUserType() != null ? user.getUserType().name() : null
            );

            return new LoginResponse(
                    token,
                    user.getEmail(),
                    user.getRole().name(),
                    user.getUserType() != null ? user.getUserType().name() : null,
                    tenantId
            );
        } catch (BadCredentialsException ex) {
            throw new RuntimeException("Invalid credentials");
        } finally {
            TenantContext.clear();
        }
    }

    private String normalizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return "public";
        }
        return tenantId.trim();
    }

    private String resolveSchema(String tenantId) {
        if ("public".equalsIgnoreCase(tenantId)) {
            return "public";
        }

        tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        return "tenant_" + tenantId;
    }

    private void validateTenantRoleCombination(User user, String tenantId) {
        if (user.getRole() == Role.SUPER_ADMIN && !"public".equalsIgnoreCase(tenantId)) {
            throw new RuntimeException("Super admin must log in through the public tenant");
        }

        if (user.getRole() != Role.SUPER_ADMIN && "public".equalsIgnoreCase(tenantId)) {
            throw new RuntimeException("Tenant users must provide a tenantId");
        }
    }
}
