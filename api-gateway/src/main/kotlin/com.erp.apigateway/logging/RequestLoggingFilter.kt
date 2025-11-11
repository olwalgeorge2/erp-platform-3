@file:Suppress("ktlint:standard:function-signature")

package com.erp.apigateway.logging

import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import org.slf4j.LoggerFactory
import org.slf4j.MDC

@Provider
@ApplicationScoped
@Priority(Priorities.USER + 100)
class RequestLoggingFilter :
    ContainerRequestFilter,
    ContainerResponseFilter {
    private val logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java)
    private val sensitiveHeaders = setOf("authorization", "proxy-authorization")

    override fun filter(requestContext: ContainerRequestContext) {
        val traceId = requestContext.getHeaderString("X-Trace-Id") ?: requestContext.getHeaderString("x-trace-id")
        if (!traceId.isNullOrBlank()) {
            MDC.put("traceId", traceId)
        }
        val method = requestContext.method
        val path = requestContext.uriInfo.requestUri.toString()
        val headers = requestContext.headers.mapValues { (k, v) -> maskHeader(k, v) }
        logger.info("incoming request method={} path={} headers={}", method, path, headers)
    }

    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext,
    ) {
        val status = responseContext.status
        val headers = responseContext.headers.mapValues { (k, v) -> maskHeader(k, v) }
        logger.info("response status={} headers={}", status, headers)
        MDC.clear()
    }

    private fun maskHeader(name: String, values: List<Any>): List<Any> =
        if (sensitiveHeaders.contains(name.lowercase())) listOf("***") else values
}
