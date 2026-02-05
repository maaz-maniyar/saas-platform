package com.maaz.saasPlatform.tenant.service;

import com.maaz.saasPlatform.audit.annotation.Auditable;
import com.maaz.saasPlatform.tenant.context.TenantContext;
import com.maaz.saasPlatform.tenant.entity.Tenant;
import com.maaz.saasPlatform.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantProvisioningService provisioningService;

    public TenantService(
            TenantRepository tenantRepository,
            TenantProvisioningService provisioningService) {
        this.tenantRepository = tenantRepository;
        this.provisioningService = provisioningService;
    }

    @Auditable(
            action = "CREATE_TENANT",
            resource = "TENANT"
    )
    @Transactional
    public Tenant createTenant(String tenantId) {

        TenantContext.setTenant("public");

        try {
            return tenantRepository.findByTenantId(tenantId)
                    .orElseGet(() -> {
                        String schema = "tenant_" + tenantId;

                        provisioningService.createSchema(schema);

                        Tenant tenant = new Tenant();
                        tenant.setTenantId(tenantId);
                        tenant.setSchemaName(schema);
                        tenant.setActive(true);

                        return tenantRepository.save(tenant);
                    });
        } finally {
            TenantContext.clear();
        }
    }
}
