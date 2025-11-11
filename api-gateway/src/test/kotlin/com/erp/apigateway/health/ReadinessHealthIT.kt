package com.erp.apigateway.health

import com.erp.apigateway.infrastructure.RedisTestResource
import com.erp.apigateway.routing.RouteWireMockResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
@QuarkusTestResource(RedisTestResource::class)
@QuarkusTestResource(RouteWireMockResource::class)
class ReadinessHealthIT {
    @Test
    fun `readiness is UP when redis and backend health are OK`() {
        given()
            .`when`()
            .get("/q/health/ready")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"))
    }
}
