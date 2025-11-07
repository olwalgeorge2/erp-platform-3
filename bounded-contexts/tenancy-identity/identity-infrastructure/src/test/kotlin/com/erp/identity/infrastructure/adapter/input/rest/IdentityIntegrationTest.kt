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

@QuarkusTest
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
    }

    companion object {
        init {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }
    }
}
