package com.maaz.saasPlatform.config.persistence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class MultiTenantProviderConfig {

    @Bean
    public SchemaMultiTenantConnectionProvider schemaMultiTenantConnectionProvider(
            DataSource dataSource
    ) {
        return new SchemaMultiTenantConnectionProvider(dataSource);
    }
}
