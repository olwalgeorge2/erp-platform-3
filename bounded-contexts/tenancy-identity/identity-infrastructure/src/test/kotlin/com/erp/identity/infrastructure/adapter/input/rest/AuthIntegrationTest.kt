package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.infrastructure.PostgresTestResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.util.UUID

@QuarkusTest
@EnabledIfSystemProperty(named = "withContainers", matches = "true")
@QuarkusTestResource(PostgresTestResource::class)
class AuthIntegrationTest {
    @Test
    fun `happy path login returns JWT token and user details`() {
        val tenantId = provisionTenant("login-test")
        val (userId, username, email) = createAndActivateUser(tenantId, DEFAULT_PASSWORD)

        // Step 4: Happy path login with correct credentials
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId",
                  "usernameOrEmail": "$username",
                  "password": "$DEFAULT_PASSWORD"
                }
                """.trimIndent(),
            ).post("/api/auth/login")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("userId", equalTo(userId))
            .body("username", equalTo(username))
            .body("email", equalTo(email))
            .body("tenantId", equalTo(tenantId))

        // Step 5: Verify login also works with email
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId",
                  "usernameOrEmail": "$email",
                  "password": "$DEFAULT_PASSWORD"
                }
                """.trimIndent(),
            ).post("/api/auth/login")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("userId", equalTo(userId))
    }

    @Test
    fun `login failure is sanitized and returns error id`() {
        val tenantId =
            java.util.UUID
                .randomUUID()
                .toString()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId",
                  "usernameOrEmail": "unknown.user@example.com",
                  "password": "WrongPass123!"
                }
                """.trimIndent(),
            ).post("/api/auth/login")
            .then()
            .statusCode(401)
            .header("X-Error-ID", notNullValue())
            .body("message", notNullValue())
    }

    @Test
    fun `login fails when tenant id does not match user tenant`() {
        val tenantA = provisionTenant("tenant-a")
        val (userId, username, _) = createAndActivateUser(tenantA, DEFAULT_PASSWORD)
        val tenantB = provisionTenant("tenant-b")

        // Attempt to login against a different tenant should fail
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantB",
                  "usernameOrEmail": "$username",
                  "password": "$DEFAULT_PASSWORD"
                }
                """.trimIndent(),
            ).post("/api/auth/login")
            .then()
            .statusCode(401)
            .header("X-Error-ID", notNullValue())

        // Login still succeeds against the correct tenant
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantA",
                  "usernameOrEmail": "$username",
                  "password": "$DEFAULT_PASSWORD"
                }
                """.trimIndent(),
            ).post("/api/auth/login")
            .then()
            .statusCode(200)
            .body("userId", equalTo(userId))
    }

    private fun provisionTenant(prefix: String): String {
        val slug = "$prefix-${UUID.randomUUID().toString().take(8)}"
        val location =
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(
                    """
                    {
                      "name": "Tenant $slug",
                      "slug": "$slug",
                      "subscription": {
                        "plan": "STARTER",
                        "startDate": "2024-01-01T00:00:00Z",
                        "maxUsers": 25,
                        "maxStorage": 2048,
                        "features": ["rbac"]
                      }
                    }
                    """.trimIndent(),
                ).post("/api/tenants")
                .then()
                .statusCode(201)
                .extract()
                .header("Location")

        return location.substringAfterLast("/").trim()
    }

    private fun createAndActivateUser(
        tenantId: String,
        password: String,
    ): Triple<String, String, String> {
        val username = "testuser${System.currentTimeMillis()}"
        val email = "testuser.${System.currentTimeMillis()}@example.com"

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
                      "fullName": "Test User",
                      "password": "$password"
                    }
                    """.trimIndent(),
                ).post("/api/auth/users")
                .then()
                .statusCode(201)
                .body("username", equalTo(username))
                .body("email", equalTo(email))
                .body("tenantId", equalTo(tenantId))
                .extract()

        val userId: String = userResponse.path("id")

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

        return Triple(userId, username, email)
    }

    companion object {
        init {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }

        private const val DEFAULT_PASSWORD = "SecurePass123!"
    }
}
