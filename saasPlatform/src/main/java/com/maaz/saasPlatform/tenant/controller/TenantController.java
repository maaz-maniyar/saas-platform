package com.maaz.saasPlatform.tenant.controller;

import com.maaz.saasPlatform.tenant.entity.Tenant;
import com.maaz.saasPlatform.tenant.service.TenantService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public Tenant createTenant(@RequestParam String tenantId) {
        return tenantService.createTenant(tenantId);
    }
}
