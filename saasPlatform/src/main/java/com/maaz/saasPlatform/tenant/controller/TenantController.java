package com.maaz.saasPlatform.tenant.controller;

import com.maaz.saasPlatform.tenant.entity.Tenant;
import com.maaz.saasPlatform.tenant.service.TenantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/platform/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<Tenant> getAllTenants() {
        return tenantService.getAllTenants();
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Tenant createTenant(@RequestParam String tenantId) {
        return tenantService.createTenant(tenantId);
    }
}
