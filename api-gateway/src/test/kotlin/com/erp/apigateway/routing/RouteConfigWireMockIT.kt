package com.erp.apigateway.routing

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
@QuarkusTestResource(RouteWireMockResource::class)
class RouteConfigWireMockIT {
    @Test
    fun `rewrites identity route and forwards`() {
        given()
            .`when`()
            .get("/api/v1/identity/echo")
            .then()
            .statusCode(200)
            .body(equalTo("ok"))
    }
}
