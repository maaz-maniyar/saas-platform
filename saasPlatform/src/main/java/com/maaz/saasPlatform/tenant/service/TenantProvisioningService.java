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
            ensureTenantSchemaObjects(stmt, schemaName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create schema " + schemaName, e);
        }
    }

    public void ensureTenantSchema(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            ensureTenantSchemaObjects(stmt, schemaName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update schema " + schemaName, e);
        }
    }

    private void ensureTenantSchemaObjects(Statement stmt, String schemaName) throws Exception {
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS %s.users (
                    id BIGSERIAL PRIMARY KEY,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(50) NOT NULL,
                    user_type VARCHAR(50)
                )
                """.formatted(schemaName));
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS %s.bug_sessions (
                    id UUID PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    description VARCHAR(2000),
                    status VARCHAR(50),
                    created_by VARCHAR(255) NOT NULL,
                    assigned_to VARCHAR(255),
                    video_s3_key VARCHAR(1000),
                    video_url VARCHAR(1200),
                    video_content_type VARCHAR(255),
                    video_size_bytes BIGINT,
                    video_upload_status VARCHAR(50) NOT NULL DEFAULT 'NOT_UPLOADED',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schemaName));
        stmt.execute("""
                ALTER TABLE %s.bug_sessions
                ADD COLUMN IF NOT EXISTS assigned_to VARCHAR(255)
                """.formatted(schemaName));
        stmt.execute("""
                ALTER TABLE %s.bug_sessions
                ADD COLUMN IF NOT EXISTS video_s3_key VARCHAR(1000)
                """.formatted(schemaName));
        stmt.execute("""
                ALTER TABLE %s.bug_sessions
                ADD COLUMN IF NOT EXISTS video_url VARCHAR(1200)
                """.formatted(schemaName));
        stmt.execute("""
                ALTER TABLE %s.bug_sessions
                ADD COLUMN IF NOT EXISTS video_content_type VARCHAR(255)
                """.formatted(schemaName));
        stmt.execute("""
                ALTER TABLE %s.bug_sessions
                ADD COLUMN IF NOT EXISTS video_size_bytes BIGINT
                """.formatted(schemaName));
        stmt.execute("""
                ALTER TABLE %s.bug_sessions
                ADD COLUMN IF NOT EXISTS video_upload_status VARCHAR(50) NOT NULL DEFAULT 'NOT_UPLOADED'
                """.formatted(schemaName));
    }
}
