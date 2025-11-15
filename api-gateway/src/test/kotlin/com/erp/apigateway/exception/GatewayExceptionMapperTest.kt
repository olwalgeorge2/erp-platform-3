package com.erp.apigateway.exception

import com.erp.apigateway.routing.RouteNotFoundException
import com.erp.apigateway.validation.GatewayValidationErrorCode
import com.erp.apigateway.validation.GatewayValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class GatewayExceptionMapperTest {
    private val mapper = GatewayExceptionMapper()

    @Test
    fun mapsRouteNotFoundTo404() {
        val response = mapper.toResponse(RouteNotFoundException("No route"))
        assertEquals(404, response.status)
        val entity = response.entity as ErrorResponse
        assertEquals("ROUTE_NOT_FOUND", entity.code)
    }

    @Test
    fun mapsValidationExceptionTo422() {
        val ex =
            GatewayValidationException(
                errorCode = GatewayValidationErrorCode.GATEWAY_INVALID_TENANT_ID,
                field = "tenant",
                rejectedValue = " ",
                locale = Locale.US,
                message = "Tenant identifier is invalid.",
            )

        val response = mapper.toResponse(ex)
        assertEquals(422, response.status)
        val entity = response.entity as ErrorResponse
        assertEquals("GATEWAY_INVALID_TENANT_ID", entity.code)
        assertEquals(1, entity.validationErrors.size)
        assertEquals("tenant", entity.validationErrors.first().field)
    }
}
