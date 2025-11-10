package com.erp.apigateway.tracing

import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import java.util.UUID

@Provider
@Priority(Priorities.HEADER_DECORATOR)
class TracingFilter :
    ContainerRequestFilter,
    ContainerResponseFilter {
    @Inject
    lateinit var traceContext: TraceContext

    override fun filter(requestContext: ContainerRequestContext) {
        val traceId = requestContext.getHeaderString("X-Trace-Id") ?: UUID.randomUUID().toString()
        // Save to request context and ensure downstream can read
        traceContext.traceId = traceId
        requestContext.headers.putSingle("X-Trace-Id", traceId)
    }

    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext,
    ) {
        val id = traceContext.traceId ?: requestContext.getHeaderString("X-Trace-Id")
        if (!id.isNullOrBlank()) {
            responseContext.headers.putSingle("X-Trace-Id", id)
        }
    }
}
