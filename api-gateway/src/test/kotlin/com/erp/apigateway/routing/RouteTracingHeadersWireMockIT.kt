package com.erp.apigateway.routing

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
@QuarkusTestResource(RouteWireMockResource::class)
class RouteTracingHeadersWireMockIT {
    @Test
    fun `passes Authorization and W3C trace headers upstream`() {
        given()
            .header("Authorization", "Bearer testtoken")
            .header("traceparent", "00-11111111111111111111111111111111-2222222222222222-01")
            .header("tracestate", "vendor=foo")
            .`when`()
            .get("/api/v1/identity/trace")
            .then()
            .statusCode(200)
            .body(equalTo("trace-ok"))
    }
}
