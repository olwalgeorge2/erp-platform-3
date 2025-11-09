package com.erp.apigateway.exception

import com.erp.apigateway.routing.RouteNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GatewayExceptionMapperTest {
    private val mapper = GatewayExceptionMapper()

    @Test
    fun mapsRouteNotFoundTo404() {
        val response = mapper.toResponse(RouteNotFoundException("No route"))
        assertEquals(404, response.status)
        val entity = response.entity as ErrorResponse
        assertEquals("ROUTE_NOT_FOUND", entity.code)
    }
}
