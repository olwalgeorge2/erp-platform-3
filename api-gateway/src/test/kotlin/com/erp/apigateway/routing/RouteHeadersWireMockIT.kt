package com.erp.apigateway.routing

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

@QuarkusTest
@EnabledIfSystemProperty(named = "withContainers", matches = "true")
@QuarkusTestResource(RouteWireMockResource::class)
class RouteHeadersWireMockIT {
    @Test
    fun `forwards headers and filters hop-by-hop on response`() {
        given()
            .header("X-Test-Header", "foo")
            .`when`()
            .get("/api/v1/identity/headers")
            .then()
            .statusCode(200)
            .header("X-Upstream", equalTo("bar"))
            .header("Connection", nullValue())
            .header("Transfer-Encoding", nullValue())
    }
}
