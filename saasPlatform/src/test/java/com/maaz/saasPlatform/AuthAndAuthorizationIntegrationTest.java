package com.maaz.saasPlatform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "resilience4j.ratelimiter.instances.global-api.limit-for-period=1000",
                "resilience4j.ratelimiter.instances.global-api.limit-refresh-period=1s",
                "resilience4j.ratelimiter.instances.global-api.timeout-duration=0"
        }
)
class AuthAndAuthorizationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final List<String> tenantIdsToCleanup = new ArrayList<>();

    @AfterEach
    void cleanupTenants() {
        for (String tenantId : tenantIdsToCleanup) {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS tenant_" + tenantId + " CASCADE");
            jdbcTemplate.update("DELETE FROM public.tenants WHERE tenant_id = ?", tenantId);
        }
        tenantIdsToCleanup.clear();
    }

    @Test
    void fullRoleFlowWorksAgainstDatabase() throws Exception {
        String tenantId = "it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        tenantIdsToCleanup.add(tenantId);

        String superAdminToken = login("superadmin@platform.com", "ChangeMe123!", "public");

        JsonNode tenantResponse = send(
                "POST",
                "/platform/tenants?tenantId=" + tenantId,
                superAdminToken,
                null,
                null,
                200
        );
        assertThat(tenantResponse.get("tenantId").asText()).isEqualTo(tenantId);

        Integer tenantCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.tenants WHERE tenant_id = ?",
                Integer.class,
                tenantId
        );
        assertThat(tenantCount).isEqualTo(1);

        createTenantUser(superAdminToken, tenantId, "admin@" + tenantId + ".com", "Admin123!", "TENANT_ADMIN", null);
        createTenantUser(superAdminToken, tenantId, "tester@" + tenantId + ".com", "Tester123!", "USER", "TESTER");
        createTenantUser(superAdminToken, tenantId, "dev@" + tenantId + ".com", "Dev123!", "USER", "DEV");

        String tenantAdminToken = login("admin@" + tenantId + ".com", "Admin123!", tenantId);
        String testerToken = login("tester@" + tenantId + ".com", "Tester123!", tenantId);
        String devToken = login("dev@" + tenantId + ".com", "Dev123!", tenantId);

        JsonNode tenantUsers = send("GET", "/tenant/users", tenantAdminToken, null, null, 200);
        assertThat(tenantUsers).hasSize(3);

        JsonNode createdBug = send(
                "POST",
                "/api/bugs",
                testerToken,
                null,
                """
                        {
                          "title": "Login page bug",
                          "description": "Broken login flow"
                        }
                        """,
                200
        );
        assertThat(createdBug.get("title").asText()).isEqualTo("Login page bug");
        String bugId = createdBug.get("id").asText();

        JsonNode bugList = send("GET", "/api/bugs", testerToken, null, null, 200);
        assertThat(bugList).hasSize(1);

        JsonNode replacedBug = send(
                "PUT",
                "/api/bugs/" + bugId,
                testerToken,
                null,
                """
                        {
                          "title": "Login page bug updated",
                          "description": "Updated by tester",
                          "status": "IN_PROGRESS"
                        }
                        """,
                200
        );
        assertThat(replacedBug.get("status").asText()).isEqualTo("IN_PROGRESS");

        JsonNode patchedBug = send(
                "PATCH",
                "/api/bugs/" + bugId,
                devToken,
                null,
                """
                        {
                          "status": "RESOLVED"
                        }
                        """,
                200
        );
        assertThat(patchedBug.get("status").asText()).isEqualTo("RESOLVED");

        send(
                "POST",
                "/api/bugs",
                devToken,
                null,
                """
                        {
                          "title": "Dev should not create",
                          "description": "Forbidden"
                        }
                        """,
                403
        );

        send(
                "PUT",
                "/api/bugs/" + bugId,
                devToken,
                null,
                """
                        {
                          "title": "Dev should not replace",
                          "description": "Forbidden",
                          "status": "OPEN"
                        }
                        """,
                403
        );

        JsonNode platformTenantUsers = send(
                "GET",
                "/platform/tenants/" + tenantId + "/users",
                superAdminToken,
                null,
                null,
                200
        );
        assertThat(platformTenantUsers).hasSize(3);

        send("GET", "/platform/users", tenantAdminToken, null, null, 403);

        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenant_" + tenantId + ".users",
                Integer.class
        );
        Integer bugCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenant_" + tenantId + ".bug_sessions",
                Integer.class
        );

        assertThat(userCount).isEqualTo(3);
        assertThat(bugCount).isEqualTo(1);
    }

    @Test
    void tenantMismatchIsRejected() throws Exception {
        String tenantOne = "it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String tenantTwo = "it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        tenantIdsToCleanup.add(tenantOne);
        tenantIdsToCleanup.add(tenantTwo);

        String superAdminToken = login("superadmin@platform.com", "ChangeMe123!", "public");

        send("POST", "/platform/tenants?tenantId=" + tenantOne, superAdminToken, null, null, 200);
        send("POST", "/platform/tenants?tenantId=" + tenantTwo, superAdminToken, null, null, 200);

        createTenantUser(superAdminToken, tenantOne, "tester@" + tenantOne + ".com", "Tester123!", "USER", "TESTER");
        String testerToken = login("tester@" + tenantOne + ".com", "Tester123!", tenantOne);

        send("GET", "/api/bugs", testerToken, tenantTwo, null, 403);
    }

    private void createTenantUser(
            String superAdminToken,
            String tenantId,
            String email,
            String password,
            String role,
            String userType
    ) throws Exception {
        String payload;
        if (userType == null) {
            payload = """
                    {
                      "email": "%s",
                      "password": "%s",
                      "role": "%s"
                    }
                    """.formatted(email, password, role);
        } else {
            payload = """
                    {
                      "email": "%s",
                      "password": "%s",
                      "role": "%s",
                      "userType": "%s"
                    }
                    """.formatted(email, password, role, userType);
        }

        send(
                "POST",
                "/platform/tenants/" + tenantId + "/users",
                superAdminToken,
                null,
                payload,
                201
        );
    }

    private String login(String email, String password, String tenantId) throws Exception {
        JsonNode response = send(
                "POST",
                "/auth/login",
                null,
                null,
                """
                        {
                          "email": "%s",
                          "password": "%s",
                          "tenantId": "%s"
                        }
                        """.formatted(email, password, tenantId),
                200
        );
        return response.get("accessToken").asText();
    }

    private JsonNode send(
            String method,
            String path,
            String token,
            String tenantHeader,
            String body,
            int expectedStatus
    ) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path));

        if (body != null) {
            requestBuilder.header("Content-Type", "application/json");
            requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        if (token != null) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        if (tenantHeader != null) {
            requestBuilder.header("X-Tenant-ID", tenantHeader);
        }

        HttpResponse<String> response = httpClient.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode())
                .as("Unexpected response body: %s", response.body())
                .isEqualTo(expectedStatus);

        if (response.body() == null || response.body().isBlank()) {
            return null;
        }

        return objectMapper.readTree(response.body());
    }
}
