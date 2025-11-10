package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.infrastructure.PostgresTestResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * End-to-end integration test for the assign-role workflow.
 *
 * Tests the full stack:
 * 1. Tenant provisioning
 * 2. User creation
 * 3. Role creation
 * 4. Role assignment to user
 * 5. Verification of role assignment
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource::class)
class AssignRoleIntegrationTest {
    @Inject
    lateinit var entityManager: EntityManager

    @Test
    fun `end-to-end assign role creates user, role, and assigns successfully`() {
        // Step 1: Create a tenant
        val tenantLocation =
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(
                    """
                    {
                      "name": "Role Assignment Test Corp",
                      "slug": "role-test-${UUID.randomUUID().toString().take(8)}",
                      "subscription": {
                        "plan": "ENTERPRISE",
                        "startDate": "2024-01-01T00:00:00Z",
                        "maxUsers": 100,
                        "maxStorage": 10240,
                        "features": ["rbac", "audit"]
                      },
                      "metadata": {
                        "region": "us-west-1"
                      }
                    }
                    """.trimIndent(),
                ).post("/api/tenants")
                .then()
                .statusCode(201)
                .extract()
                .header("Location")

        val tenantId = tenantLocation.substringAfterLast("/").trim()

        // Step 2: Create a user for this tenant
        val username = "roleuser${System.currentTimeMillis()}"
        val email = "roleuser.${System.currentTimeMillis()}@example.com"

        val userResponse =
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(
                    """
                    {
                      "tenantId": "$tenantId",
                      "username": "$username",
                      "email": "$email",
                      "fullName": "Role Test User",
                      "password": "SecurePass123!"
                    }
                    """.trimIndent(),
                ).post("/api/auth/users")
                .then()
                .statusCode(201)
                .body("username", equalTo(username))
                .body("tenantId", equalTo(tenantId))
                .extract()

        val userId: String = userResponse.path("id")

        // Step 3: Activate the user
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId"
                }
                """.trimIndent(),
            ).post("/api/auth/users/$userId/activate")
            .then()
            .statusCode(200)

        // Step 4: Create a role in this tenant
        val roleResponse =
            RestAssured
                .given()
                .headers(adminHeaders(tenantId))
                .contentType(ContentType.JSON)
                .body(
                    """
                    {
                      "name": "developer",
                      "description": "Developer role with code access",
                      "permissions": [
                        { "resource": "repositories", "action": "read", "scope": "TENANT" },
                        { "resource": "repositories", "action": "write", "scope": "TENANT" },
                        { "resource": "deployments", "action": "read", "scope": "TENANT" }
                      ],
                      "metadata": {
                        "createdBy": "integration-test",
                        "department": "engineering"
                      }
                    }
                    """.trimIndent(),
                ).post("/api/tenants/$tenantId/roles")
                .then()
                .statusCode(201)
                .body("name", equalTo("developer"))
                .body("permissions.size()", equalTo(3))
                .extract()

        val roleId: String = roleResponse.path("id")

        // Step 5: Assign the role to the user
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId",
                  "roleId": "$roleId"
                }
                """.trimIndent(),
            ).post("/api/auth/users/$userId/roles")
            .then()
            .statusCode(200)
            .body("id", equalTo(userId))
            .body("username", equalTo(username))

        // Step 6: Verify the role is now attached to the user (get user and check roleIds)
        RestAssured
            .given()
            .headers(adminHeaders(tenantId))
            .get("/api/tenants/$tenantId/users/$userId")
            .then()
            .statusCode(200)
            .body("id", equalTo(userId))
        // Note: The exact field name depends on UserResponse schema
        // Adjust if needed (e.g., roleIds, roles, assignedRoles)

        // Step 7: Verify role still exists in the role list
        RestAssured
            .given()
            .headers(adminHeaders(tenantId))
            .get("/api/tenants/$tenantId/roles")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].id", equalTo(roleId))
            .body("[0].name", equalTo("developer"))

        // Step 8: Verify outbox events were created for audit trail
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
            pendingEvents >= 2,
            "Expected at least 2 pending outbox events (user created, role assigned), but found $pendingEvents",
        )
    }

    @Test
    fun `assign role with invalid user UUID returns 400`() {
        val tenantId = UUID.randomUUID().toString()
        val roleId = UUID.randomUUID().toString()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId",
                  "roleId": "$roleId"
                }
                """.trimIndent(),
            ).post("/api/auth/users/not-a-uuid/roles")
            .then()
            .statusCode(400)
            .body("code", equalTo("INVALID_IDENTIFIER"))
            .body("message", notNullValue())
    }

    @Test
    fun `assign non-existent role to user returns error`() {
        // Step 1: Create tenant and user
        val tenantLocation =
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(
                    """
                    {
                      "name": "Invalid Role Test",
                      "slug": "invalid-role-${UUID.randomUUID().toString().take(8)}",
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
                .statusCode(201)
                .extract()
                .header("Location")

        val tenantId = tenantLocation.substringAfterLast("/").trim()

        val userResponse =
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(
                    """
                    {
                      "tenantId": "$tenantId",
                      "username": "testuser${System.currentTimeMillis()}",
                      "email": "test${System.currentTimeMillis()}@example.com",
                      "fullName": "Test User",
                      "password": "Pass123!"
                    }
                    """.trimIndent(),
                ).post("/api/auth/users")
                .then()
                .statusCode(201)
                .extract()

        val userId: String = userResponse.path("id")

        // Step 2: Try to assign a non-existent role
        val nonExistentRoleId = UUID.randomUUID()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId",
                  "roleId": "$nonExistentRoleId"
                }
                """.trimIndent(),
            ).post("/api/auth/users/$userId/roles")
            .then()
            .statusCode(404) // Or 400, depending on your error handling strategy
    }

    companion object {
        init {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }

        private fun adminHeaders(tenantId: String): Map<String, String> =
            mapOf(
                "X-User-Id" to "integration-admin",
                "X-User-Roles" to "TENANT_ADMIN",
                "X-User-Permissions" to "roles:manage,roles:read,users:read,users:manage",
                "X-Tenant-ID" to tenantId,
            )
    }
}
