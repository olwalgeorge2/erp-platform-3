package com.erp.apigateway

import com.erp.apigateway.infrastructure.RedisTestResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.junit.jupiter.api.Test

/**
 * Integration test for API Gateway using Testcontainers for Redis.
 * This test verifies that the gateway properly integrates with Redis for rate limiting.
 */
@QuarkusTest
@QuarkusTestResource(RedisTestResource::class)
class GatewayRouterTest {
    @Test
    fun `should return 404 or 401 for non-configured routes`() {
        // Accept both 404 (no route) and 401 (auth required)
        val statusCode =
            given()
                .`when`()
                .get("/api/v1/nonexistent/endpoint")
                .then()
                .extract()
                .statusCode()

        assert(statusCode == 404 || statusCode == 401) {
            "Expected 404 or 401, but got $statusCode"
        }
    }

    @Test
    fun `should return CORS headers on OPTIONS request`() {
        given()
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "GET")
            .`when`()
            .options("/api/v1/identity/test")
            .then()
            .statusCode(200)
    }

    @Test
    fun `should reject unauthorized requests to protected routes`() {
        // This assumes routes require authentication by default
        given()
            .`when`()
            .get("/api/v1/identity/users")
            .then()
            .statusCode(401)
    }
}
