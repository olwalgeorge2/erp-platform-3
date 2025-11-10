package com.erp.apigateway.exception

import com.erp.apigateway.routing.RouteNotFoundException
import com.erp.apigateway.tracing.TraceContext
import jakarta.inject.Inject
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

data class ErrorResponse(
    val code: String,
    val message: String,
    val traceId: String? = null,
)

@Provider
class GatewayExceptionMapper : ExceptionMapper<Throwable> {
    @Inject
    var traceContext: TraceContext? = null

    override fun toResponse(exception: Throwable): Response {
        val (status, code, message) =
            when (exception) {
                is RouteNotFoundException ->
                    Triple(
                        Response.Status.NOT_FOUND,
                        "ROUTE_NOT_FOUND",
                        exception.message ?: "Route not found",
                    )

                else ->
                    Triple(
                        Response.Status.INTERNAL_SERVER_ERROR,
                        "INTERNAL_ERROR",
                        "An unexpected error occurred",
                    )
            }

        val body = ErrorResponse(code = code, message = message, traceId = traceContext?.traceId)

        return Response
            .status(status)
            .entity(body)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .build()
    }
}
