package com.maaz.saasPlatform.tenant.service;

import javax.sql.DataSource;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.Statement;

@Service
public class TenantProvisioningService {

    private final DataSource dataSource;

    public TenantProvisioningService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createSchema(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {

            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create schema " + schemaName, e);
        }
    }
}
