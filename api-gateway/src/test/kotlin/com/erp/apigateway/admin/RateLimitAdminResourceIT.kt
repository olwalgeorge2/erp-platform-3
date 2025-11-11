package com.erp.apigateway.admin

import com.erp.apigateway.infrastructure.RedisTestResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.smallrye.jwt.build.Jwt
import jakarta.json.Json
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date

@QuarkusTest
@QuarkusTestResource(RedisTestResource::class)
@QuarkusTestResource(AdminJwtTestResource::class)
class RateLimitAdminResourceIT {
    private fun adminToken(): String =
        Jwt
            .claims()
            .issuer("erp-platform-dev")
            .subject("admin-user")
            .groups(setOf("admin"))
            .issuedAt(Instant.now())
            .expiresAt(Date.from(Instant.now().plusSeconds(600)))
            .jws()
            .sign(AdminKeys.privateKey)

    private fun userToken(): String =
        Jwt
            .claims()
            .issuer("erp-platform-dev")
            .subject("user")
            .groups(setOf("user"))
            .issuedAt(Instant.now())
            .expiresAt(Date.from(Instant.now().plusSeconds(600)))
            .jws()
            .sign(AdminKeys.privateKey)

    @Test
    fun admin_can_manage_tenant_override() {
        val token = adminToken()
        val body =
            Json
                .createObjectBuilder()
                .add("requestsPerMinute", 250)
                .add("windowSeconds", 60)
                .build()
                .toString()

        given()
            .header("Authorization", "Bearer $token")
            .contentType("application/json")
            .body(body)
            .`when`()
            .put("/admin/ratelimits/tenants/acme")
            .then()
            .statusCode(204)

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("/admin/ratelimits/tenants/acme")
            .then()
            .statusCode(200)
            .body("tenant", equalTo("acme"))
            .body("requestsPerMinute", equalTo(250))
            .body("windowSeconds", equalTo(60))

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .delete("/admin/ratelimits/tenants/acme")
            .then()
            .statusCode(204)
    }

    @Test
    fun non_admin_gets_403() {
        val token = userToken()
        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("/admin/ratelimits/tenants")
            .then()
            .statusCode(403)
    }

    @Test
    fun admin_can_manage_endpoint_override_and_list() {
        val token = adminToken()
        val body =
            Json
                .createObjectBuilder()
                .add("requestsPerMinute", 7)
                .add("windowSeconds", 60)
                .build()
                .toString()

        val pattern = "/api/v1/identity/*"

        given()
            .header("Authorization", "Bearer $token")
            .contentType("application/json")
            .body(body)
            .`when`()
            .put("/admin/ratelimits/endpoints/$pattern")
            .then()
            .statusCode(204)

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("/admin/ratelimits/endpoints/$pattern")
            .then()
            .statusCode(200)
            .body("pattern", equalTo(pattern))
            .body("requestsPerMinute", equalTo(7))
            .body("windowSeconds", equalTo(60))

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("/admin/ratelimits/endpoints")
            .then()
            .statusCode(200)

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .delete("/admin/ratelimits/endpoints/$pattern")
            .then()
            .statusCode(204)
    }
}
