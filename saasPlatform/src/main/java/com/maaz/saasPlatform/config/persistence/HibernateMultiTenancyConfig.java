package com.maaz.saasPlatform.config.persistence;

import com.maaz.saasPlatform.tenant.resolver.HeaderTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class HibernateMultiTenancyConfig
        implements HibernatePropertiesCustomizer {

    private final SchemaMultiTenantConnectionProvider connectionProvider;
    private final HeaderTenantIdentifierResolver tenantResolver;

    public HibernateMultiTenancyConfig(
            SchemaMultiTenantConnectionProvider connectionProvider,
            HeaderTenantIdentifierResolver tenantResolver
    ) {
        this.connectionProvider = connectionProvider;
        this.tenantResolver = tenantResolver;
    }

    @Override
    public void customize(Map<String, Object> properties) {
        properties.put(
                "hibernate.multi_tenant_connection_provider",
                connectionProvider
        );
        properties.put(
                "hibernate.tenant_identifier_resolver",
                tenantResolver
        );
    }
}
