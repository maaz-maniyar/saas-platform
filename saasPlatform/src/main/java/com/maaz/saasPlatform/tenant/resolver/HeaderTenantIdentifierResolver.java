package com.maaz.saasPlatform.tenant.resolver;

import com.maaz.saasPlatform.tenant.context.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class HeaderTenantIdentifierResolver
        implements CurrentTenantIdentifierResolver {

    private static final String DEFAULT_TENANT = "public";

    // REQUIRED: explicit no-arg constructor (Hibernate instantiates this)
    public HeaderTenantIdentifierResolver() {
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getTenant();
        System.out.println("Resolved tenant = " + tenant);
        return tenant != null ? tenant : DEFAULT_TENANT;
    }


    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
