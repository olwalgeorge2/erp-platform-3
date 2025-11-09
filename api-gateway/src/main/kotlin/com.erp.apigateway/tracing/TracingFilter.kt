package com.erp.apigateway.tracing

import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.ext.Provider
import java.util.UUID

@Provider
@Priority(Priorities.HEADER_DECORATOR)
class TracingFilter : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        val traceId = requestContext.getHeaderString("X-Trace-Id") ?: UUID.randomUUID().toString()
        requestContext.headers.putSingle("X-Trace-Id", traceId)
    }
}
