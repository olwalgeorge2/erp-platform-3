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
class UserCredentialIntegrationTest {
    @Test
    fun `update credentials rejects invalid current password`() {
        val tenantId = provisionTenant("creds-reject")
        val (userId, username, email) = createAndActivateUser(tenantId)

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId",
                  "currentPassword": "WrongPassword123!",
                  "newPassword": "ValidReset123!",
                  "requestedBy": "integration-suite"
                }
                """.trimIndent(),
            ).put("/api/auth/users/$userId/credentials")
            .then()
            .statusCode(400)
            .body("code", equalTo("INVALID_CREDENTIALS"))
            .body("message", notNullValue())

        // Ensure the original password still works after the rejected attempt
        login(tenantId, username, INITIAL_PASSWORD)
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("email", equalTo(email))
    }

    @Test
    fun `user can update credentials and authenticate with new password`() {
        val tenantId = provisionTenant("creds-rotate")
        val (userId, username, _) = createAndActivateUser(tenantId)

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId",
                  "currentPassword": "$INITIAL_PASSWORD",
                  "newPassword": "$ROTATED_PASSWORD",
                  "requestedBy": "integration-suite"
                }
                """.trimIndent(),
            ).put("/api/auth/users/$userId/credentials")
            .then()
            .statusCode(200)
            .body("failedLoginAttempts", equalTo(0))
            .body("lockedUntil", equalTo(null))
            .body("status", equalTo("ACTIVE"))

        // Old password should now be rejected
        login(tenantId, username, INITIAL_PASSWORD)
            .then()
            .statusCode(401)

        // New password succeeds
        login(tenantId, username, ROTATED_PASSWORD)
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("username", equalTo(username))
    }

    @Test
    fun `suspend user blocks login until reactivated`() {
        val tenantId = provisionTenant("creds-suspend")
        val (userId, username, _) = createAndActivateUser(tenantId)

        suspendUser(tenantId, userId, reason = "policy violation")

        login(tenantId, username, INITIAL_PASSWORD)
            .then()
            .statusCode(403)
            .body("code", equalTo("USER_NOT_ALLOWED"))

        reactivateUser(tenantId, userId)

        login(tenantId, username, INITIAL_PASSWORD)
            .then()
            .statusCode(200)
            .body("username", equalTo(username))
    }

    @Test
    fun `admin reset password allows immediate login when change not required`() {
        val tenantId = provisionTenant("creds-reset-nochange")
        val (userId, username, _) = createAndActivateUser(tenantId)

        resetPassword(
            tenantId = tenantId,
            userId = userId,
            newPassword = RESET_PASSWORD,
            requireChange = false,
        )

        // Old password blocked
        login(tenantId, username, INITIAL_PASSWORD)
            .then()
            .statusCode(401)

        // New password works immediately
        login(tenantId, username, RESET_PASSWORD)
            .then()
            .statusCode(200)
            .body("username", equalTo(username))
    }

    @Test
    fun `admin reset password enforces change when require flag true`() {
        val tenantId = provisionTenant("creds-reset-require")
        val (userId, username, _) = createAndActivateUser(tenantId)

        resetPassword(
            tenantId = tenantId,
            userId = userId,
            newPassword = RESET_PASSWORD,
            requireChange = true,
        )

        // Login blocked until password change completed
        login(tenantId, username, RESET_PASSWORD)
            .then()
            .statusCode(403)
            .body("code", equalTo("USER_NOT_ALLOWED"))
    }

    private fun createAndActivateUser(tenantId: String): Triple<String, String, String> {
        val username = "integration_${UUID.randomUUID().toString().take(8)}"
        val email = "$username@example.com"

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
                      "fullName": "Integration Test User",
                      "password": "$INITIAL_PASSWORD"
                    }
                    """.trimIndent(),
                ).post("/api/auth/users")
                .then()
                .statusCode(201)
                .body("username", equalTo(username))
                .body("email", equalTo(email))
                .extract()

        val userId: String = userResponse.path("id")

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId",
                  "requestedBy": "integration-suite",
                  "requirePasswordReset": false
                }
                """.trimIndent(),
            ).post("/api/auth/users/$userId/activate")
            .then()
            .statusCode(200)

        return Triple(userId, username, email)
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
                      "name": "Integration Tenant $slug",
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

    private fun login(
        tenantId: String,
        usernameOrEmail: String,
        password: String,
    ) = RestAssured
        .given()
        .contentType(ContentType.JSON)
        .body(
            """
            {
                "tenantId": "$tenantId",
                "usernameOrEmail": "$usernameOrEmail",
                "password": "$password"
            }
            """.trimIndent(),
        ).post("/api/auth/login")

    companion object {
        private const val INITIAL_PASSWORD = "Password123!"
        private const val ROTATED_PASSWORD = "Password456!"
        private const val RESET_PASSWORD = "ResetPassword789!"

        init {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }
    }

    private fun suspendUser(
        tenantId: String,
        userId: String,
        reason: String,
    ) {
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId",
                  "reason": "$reason"
                }
                """.trimIndent(),
            ).post("/api/auth/users/$userId/suspend")
            .then()
            .statusCode(200)
            .body("status", equalTo("SUSPENDED"))
    }

    private fun reactivateUser(
        tenantId: String,
        userId: String,
    ) {
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId"
                }
                """.trimIndent(),
            ).post("/api/auth/users/$userId/reactivate")
            .then()
            .statusCode(200)
            .body("status", equalTo("ACTIVE"))
    }

    private fun resetPassword(
        tenantId: String,
        userId: String,
        newPassword: String,
        requireChange: Boolean,
    ) {
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId",
                  "newPassword": "$newPassword",
                  "requirePasswordChange": $requireChange
                }
                """.trimIndent(),
            ).post("/api/auth/users/$userId/reset-password")
            .then()
            .statusCode(200)
            .body("id", equalTo(userId))
            .body("tenantId", equalTo(tenantId))
    }
}
