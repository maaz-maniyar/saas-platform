package com.maaz.saasPlatform.tenant.controller;

import com.maaz.saasPlatform.auth.util.JwtUtil;
import com.maaz.saasPlatform.tenant.entity.Tenant;
import com.maaz.saasPlatform.tenant.service.TenantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/tenants")
public class TenantController {

    private final TenantService tenantService;
    private final JwtUtil jwtUtil;


    public TenantController(TenantService tenantService, JwtUtil jwtUtil) {
        this.tenantService = tenantService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/token")
    public String getToken() {
        return jwtUtil.generateToken(
                "admin@platform.com",
                "public",
                "PLATFORM_ADMIN"
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Tenant createTenant(@RequestParam String tenantId) {
        return tenantService.createTenant(tenantId);
    }
}
