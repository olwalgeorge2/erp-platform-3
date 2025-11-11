package com.erp.apigateway.filter

import com.erp.apigateway.infrastructure.RedisTestResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

@QuarkusTest
@EnabledIfSystemProperty(named = "withContainers", matches = "true")
@QuarkusTestResource(RedisTestResource::class)
class RateLimitIntegrationTest {
    @Test
    fun `returns 429 after exceeding endpoint limit`() {
        // Endpoint override in test application.yml: 2 rpm for /api/v1/identity/*
        repeat(2) {
            given()
                .`when`()
                .get("/test/echo")
                .then()
                .statusCode(
                    org.hamcrest.Matchers.anyOf(equalTo(200), equalTo(201), equalTo(204), equalTo(401), equalTo(502)),
                )
        }

        // Third call should be limited
        given()
            .`when`()
            .get("/test/echo")
            .then()
            .statusCode(429)
            .body("code", equalTo("RATE_LIMIT_EXCEEDED"))
    }
}
