package com.erp.apigateway.logging

import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import org.slf4j.LoggerFactory
import java.time.Instant

@Provider
@Priority(Priorities.USER)
class RequestLoggingFilter :
    ContainerRequestFilter,
    ContainerResponseFilter {
    private val logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun filter(requestContext: ContainerRequestContext) {
        requestContext.setProperty("_start", System.currentTimeMillis())
    }

    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext,
    ) {
        val start = requestContext.getProperty("_start") as? Long ?: System.currentTimeMillis()
        val duration = System.currentTimeMillis() - start
        val path = requestContext.uriInfo.path
        val method = requestContext.method
        val status = responseContext.status
        val traceId = requestContext.getHeaderString("X-Trace-Id") ?: "-"
        logger.info(
            "{} {} status={} duration_ms={} traceId={} ts={}",
            method,
            path,
            status,
            duration,
            traceId,
            Instant.now().toString(),
        )
    }
}
