package com.maaz.saasPlatform.config.persistence;

import com.maaz.saasPlatform.tenant.service.TenantProvisioningService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantSchemaMigrationRunner implements ApplicationRunner {

    private final DataSource dataSource;
    private final TenantProvisioningService tenantProvisioningService;

    public TenantSchemaMigrationRunner(
            DataSource dataSource,
            TenantProvisioningService tenantProvisioningService
    ) {
        this.dataSource = dataSource;
        this.tenantProvisioningService = tenantProvisioningService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT schema_name
                     FROM information_schema.schemata
                     WHERE schema_name LIKE 'tenant\\_%' ESCAPE '\\'
                     """);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                tenantProvisioningService.ensureTenantSchema(resultSet.getString("schema_name"));
            }
        }
    }
}
