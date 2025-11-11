package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.infrastructure.PostgresTestResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

@QuarkusTest
@EnabledIfSystemProperty(named = "withContainers", matches = "true")
@QuarkusTestResource(PostgresTestResource::class)
class IdentityIntegrationTest {
    @Inject
    lateinit var entityManager: EntityManager

    @Test
    fun `provision tenant and create user across tenants`() {
        val tenantLocation =
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(
                    """
                    {
                      "name": "Integration One",
                      "slug": "integration-one",
                      "subscription": {
                        "plan": "STARTER",
                        "startDate": "2024-01-01T00:00:00Z",
                        "maxUsers": 25,
                        "maxStorage": 5000,
                        "features": ["rbac"]
                      },
                      "metadata": {
                        "region": "us-east-1"
                      }
                    }
                    """.trimIndent(),
                ).post("/api/tenants")
                .then()
                .statusCode(201)
                .extract()
                .header("Location")

        val otherTenantLocation =
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(
                    """
                    {
                      "name": "Integration Two",
                      "slug": "integration-two",
                      "subscription": {
                        "plan": "STARTER",
                        "startDate": "2024-01-01T00:00:00Z",
                        "maxUsers": 10,
                        "maxStorage": 2048,
                        "features": []
                      }
                    }
                    """.trimIndent(),
                ).post("/api/tenants")
                .then()
                .statusCode(201)
                .extract()
                .header("Location")

        val tenantId = tenantLocation.substringAfterLast("/").trim()
        val otherTenantId = otherTenantLocation.substringAfterLast("/").trim()

        val createUserResponse =
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(
                    """
                    {
                      "tenantId": "$tenantId",
                      "username": "integration_user",
                      "email": "integration.user@example.com",
                      "fullName": "Integration User",
                      "password": "Password123!"
                    }
                    """.trimIndent(),
                ).post("/api/auth/users")

        assertEquals(
            201,
            createUserResponse.statusCode,
            "User creation failed with body: ${createUserResponse.body.asString()}",
        )

        createUserResponse
            .then()
            .body("username", equalTo("integration_user"))

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "name": "Dup",
                  "slug": "integration-one",
                  "subscription": {
                    "plan": "STARTER",
                    "startDate": "2024-01-01T00:00:00Z",
                    "maxUsers": 5,
                    "maxStorage": 512,
                    "features": []
                  }
                }
                """.trimIndent(),
            ).post("/api/tenants")
            .then()
            .statusCode(409)

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$otherTenantId",
                  "username": "integration_user",
                  "email": "integration.user+alt@example.com",
                  "fullName": "Integration User Alt",
                  "password": "Password123!"
                }
                """.trimIndent(),
            ).post("/api/auth/users")
            .then()
            .statusCode(201)
            .body("tenantId", equalTo(otherTenantId))

        val pendingEvents =
            entityManager
                .createQuery(
                    """
                    SELECT COUNT(e) FROM OutboxEventEntity e
                    WHERE e.status = 'PENDING'
                    """.trimIndent(),
                    java.lang.Long::class.java,
                ).singleResult

        assertTrue(
            pendingEvents >= 1,
            "Expected at least one pending outbox event, but found $pendingEvents",
        )

        val roleId =
            RestAssured
                .given()
                .headers(adminHeaders(tenantId))
                .contentType(ContentType.JSON)
                .body(
                    """
                    {
                      "name": "tenant-admin",
                      "description": "Tenant administrator role",
                      "permissions": [
                        { "resource": "users", "action": "manage", "scope": "TENANT" },
                        { "resource": "roles", "action": "manage", "scope": "TENANT" }
                      ],
                      "metadata": { "createdBy": "integration-test" }
                    }
                    """.trimIndent(),
                ).post("/api/tenants/$tenantId/roles")
                .then()
                .statusCode(201)
                .extract()
                .path<String>("id")

        RestAssured
            .given()
            .headers(adminHeaders(tenantId))
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "name": "tenant-admin",
                  "description": "Updated role via integration test",
                  "permissions": [
                    { "resource": "users", "action": "manage", "scope": "TENANT" },
                    { "resource": "roles", "action": "manage", "scope": "TENANT" },
                    { "resource": "tenants", "action": "read", "scope": "TENANT" }
                  ],
                  "metadata": { "updatedBy": "integration-test" }
                }
                """.trimIndent(),
            ).put("/api/tenants/$tenantId/roles/$roleId")
            .then()
            .statusCode(200)
            .body("permissions.size()", equalTo(3))
            .body("description", equalTo("Updated role via integration test"))

        RestAssured
            .given()
            .headers(adminHeaders(tenantId))
            .get("/api/tenants/$tenantId/roles")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))

        RestAssured
            .given()
            .headers(adminHeaders(tenantId))
            .delete("/api/tenants/$tenantId/roles/$roleId")
            .then()
            .statusCode(204)

        RestAssured
            .given()
            .headers(adminHeaders(tenantId))
            .get("/api/tenants/$tenantId/roles")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0))
    }

    companion object {
        init {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }

        private fun adminHeaders(tenantId: String): Map<String, String> =
            mapOf(
                "X-User-Id" to "integration-admin",
                "X-User-Roles" to "TENANT_ADMIN",
                "X-User-Permissions" to "roles:manage,roles:read",
                "X-Tenant-ID" to tenantId,
            )
    }
}
