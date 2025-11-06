package com.erp.identity.infrastructure.web

import io.quarkus.logging.Log
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import java.time.Duration
import java.util.UUID
import org.slf4j.MDC

@Provider
class RequestLoggingFilter : ContainerRequestFilter, ContainerResponseFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        val traceId = requestContext.getHeaderString(TRACE_ID_HEADER)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val tenantId = requestContext.getHeaderString(TENANT_ID_HEADER)

        MDC.put("traceId", traceId)
        if (!tenantId.isNullOrBlank()) {
            MDC.put("tenantId", tenantId)
        }
        TenantRequestContext.set(tenantId)

        requestContext.setProperty("traceId", traceId)
        requestContext.setProperty("startTimeNano", System.nanoTime())
    }

    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext,
    ) {
        val traceId = (requestContext.getProperty("traceId") as? String) ?: MDC.get("traceId")
        if (!traceId.isNullOrBlank()) {
            responseContext.headers.add(TRACE_ID_HEADER, traceId)
        }

        val startNano = requestContext.getProperty("startTimeNano") as? Long
        if (startNano != null) {
            val durationMs = Duration.ofNanos(System.nanoTime() - startNano).toMillis()
            Log.infof(
                "[%s] HTTP %s %s -> %d (%dms)",
                traceId ?: "n/a",
                requestContext.method,
                requestContext.uriInfo?.path ?: "<unknown>",
                responseContext.status,
                durationMs,
            )
        }

        TenantRequestContext.clear()
        MDC.clear()
    }

    companion object {
        private const val TRACE_ID_HEADER = "X-Trace-ID"
        private const val TENANT_ID_HEADER = "X-Tenant-ID"
    }
}
