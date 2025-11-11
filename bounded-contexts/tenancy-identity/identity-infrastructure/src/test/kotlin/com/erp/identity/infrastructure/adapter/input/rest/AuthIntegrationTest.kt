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
        // Step 1: Create a tenant
        val tenantId = UUID.randomUUID().toString()
        val tenantLocation =
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(
                    """
                    {
                      "name": "Login Test Tenant",
                      "slug": "login-test-${UUID.randomUUID().toString().take(8)}",
                      "subscription": {
                        "plan": "STARTER",
                        "startDate": "2024-01-01T00:00:00Z",
                        "maxUsers": 10,
                        "maxStorage": 1024,
                        "features": []
                      }
                    }
                    """.trimIndent(),
                ).post("/api/tenants")
                .then()
                .statusCode(201)
                .extract()
                .header("Location")

        val createdTenantId = tenantLocation.substringAfterLast("/").trim()

        // Step 2: Create a user for this tenant
        val username = "testuser${System.currentTimeMillis()}"
        val email = "testuser.${System.currentTimeMillis()}@example.com"
        val password = "SecurePass123!"

        val userResponse =
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(
                    """
                    {
                      "tenantId": "$createdTenantId",
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
                .body("tenantId", equalTo(createdTenantId))
                .extract()

        val userId: String = userResponse.path("id")

        // Step 3: Activate the user (assuming activation is required before login)
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$createdTenantId"
                }
                """.trimIndent(),
            ).post("/api/auth/users/$userId/activate")
            .then()
            .statusCode(200)

        // Step 4: Happy path login with correct credentials
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$createdTenantId",
                  "usernameOrEmail": "$username",
                  "password": "$password"
                }
                """.trimIndent(),
            ).post("/api/auth/login")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("userId", equalTo(userId))
            .body("username", equalTo(username))
            .body("email", equalTo(email))
            .body("tenantId", equalTo(createdTenantId))

        // Step 5: Verify login also works with email
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$createdTenantId",
                  "usernameOrEmail": "$email",
                  "password": "$password"
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

    companion object {
        init {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }
    }
}
