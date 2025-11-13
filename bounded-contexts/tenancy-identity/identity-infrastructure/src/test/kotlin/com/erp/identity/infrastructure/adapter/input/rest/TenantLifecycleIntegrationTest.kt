package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.infrastructure.PostgresTestResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.util.UUID

@QuarkusTest
@EnabledIfSystemProperty(named = "withContainers", matches = "true")
@QuarkusTestResource(PostgresTestResource::class)
class TenantLifecycleIntegrationTest {
    @Test
    fun `tenant can be activated suspended and resumed`() {
        val tenantId = provisionTenant()

        // Newly provisioned tenant is in provisioning status
        getTenant(tenantId)
            .then()
            .statusCode(200)
            .body("status", equalTo("PROVISIONING"))

        // Activate the tenant
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body("""{ "requestedBy": "integration-suite" }""")
            .post("/api/tenants/$tenantId/activate")
            .then()
            .statusCode(200)
            .body("status", equalTo("ACTIVE"))

        // Suspend with a reason
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body("""{ "reason": "maintenance window", "requestedBy": "integration-suite" }""")
            .post("/api/tenants/$tenantId/suspend")
            .then()
            .statusCode(200)
            .body("status", equalTo("SUSPENDED"))
            .body("metadata.suspension_reason", equalTo("maintenance window"))

        getTenant(tenantId)
            .then()
            .statusCode(200)
            .body("status", equalTo("SUSPENDED"))
            .body("metadata.suspension_reason", equalTo("maintenance window"))

        // Resume tenant operations
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body("""{ "requestedBy": "integration-suite" }""")
            .post("/api/tenants/$tenantId/resume")
            .then()
            .statusCode(200)
            .body("status", equalTo("ACTIVE"))
            .body("metadata", not(hasKey("suspension_reason")))

        getTenant(tenantId)
            .then()
            .statusCode(200)
            .body("status", equalTo("ACTIVE"))
            .body("metadata", not(hasKey("suspension_reason")))
    }

    private fun provisionTenant(): String {
        val slug = "tenant-${UUID.randomUUID().toString().take(8)}"
        val location =
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(
                    """
                    {
                      "name": "Lifecycle Tenant $slug",
                      "slug": "$slug",
                      "subscription": {
                        "plan": "STARTER",
                        "startDate": "2024-01-01T00:00:00Z",
                        "maxUsers": 50,
                        "maxStorage": 4096,
                        "features": ["rbac"]
                      }
                    }
                    """.trimIndent(),
                ).post("/api/tenants")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .extract()
                .header("Location")

        return location.substringAfterLast("/").trim()
    }

    private fun getTenant(tenantId: String) =
        RestAssured
            .given()
            .accept(ContentType.JSON)
            .get("/api/tenants/$tenantId")

    companion object {
        init {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }
    }
}
