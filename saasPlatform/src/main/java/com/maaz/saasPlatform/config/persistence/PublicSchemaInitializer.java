package com.maaz.saasPlatform.config.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublicSchemaInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    public PublicSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {

            stmt.execute("CREATE SCHEMA IF NOT EXISTS public");
            stmt.execute("SET search_path TO public");
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS public.users (
                        id BIGSERIAL PRIMARY KEY,
                        email VARCHAR(255) NOT NULL UNIQUE,
                        password VARCHAR(255) NOT NULL,
                        role VARCHAR(50) NOT NULL,
                        user_type VARCHAR(50)
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS public.tenants (
                        id BIGSERIAL PRIMARY KEY,
                        tenant_id VARCHAR(255) NOT NULL UNIQUE,
                        schema_name VARCHAR(255) NOT NULL,
                        active BOOLEAN NOT NULL DEFAULT TRUE
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS public.audit_logs (
                        id BIGSERIAL PRIMARY KEY,
                        tenant_id VARCHAR(255),
                        user_email VARCHAR(255),
                        action VARCHAR(255),
                        resource VARCHAR(255),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS public.projects (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS public.bug_sessions (
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
                    """);
            stmt.execute("""
                    ALTER TABLE public.bug_sessions
                    ADD COLUMN IF NOT EXISTS assigned_to VARCHAR(255)
                    """);
            stmt.execute("""
                    ALTER TABLE public.bug_sessions
                    ADD COLUMN IF NOT EXISTS video_s3_key VARCHAR(1000)
                    """);
            stmt.execute("""
                    ALTER TABLE public.bug_sessions
                    ADD COLUMN IF NOT EXISTS video_url VARCHAR(1200)
                    """);
            stmt.execute("""
                    ALTER TABLE public.bug_sessions
                    ADD COLUMN IF NOT EXISTS video_content_type VARCHAR(255)
                    """);
            stmt.execute("""
                    ALTER TABLE public.bug_sessions
                    ADD COLUMN IF NOT EXISTS video_size_bytes BIGINT
                    """);
            stmt.execute("""
                    ALTER TABLE public.bug_sessions
                    ADD COLUMN IF NOT EXISTS video_upload_status VARCHAR(50) NOT NULL DEFAULT 'NOT_UPLOADED'
                    """);
        }
    }
}
