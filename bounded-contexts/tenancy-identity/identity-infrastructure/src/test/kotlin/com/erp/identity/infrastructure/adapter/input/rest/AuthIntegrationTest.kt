package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.infrastructure.PostgresTestResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test

@QuarkusTest
@QuarkusTestResource(PostgresTestResource::class)
class AuthIntegrationTest {
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
}
