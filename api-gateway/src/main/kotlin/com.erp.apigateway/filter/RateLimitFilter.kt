package com.erp.apigateway.filter

import com.erp.apigateway.context.TenantContext
import com.erp.apigateway.metrics.GatewayMetrics
import com.erp.apigateway.ratelimit.RateLimiter
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider

@Provider
@Priority(Priorities.AUTHENTICATION + 100)
class RateLimitFilter : ContainerRequestFilter {
    @Inject
    lateinit var tenantContext: TenantContext

    @Inject
    lateinit var rateLimiter: RateLimiter

    @Inject
    lateinit var metrics: GatewayMetrics

    override fun filter(requestContext: ContainerRequestContext) {
        val tenant = tenantContext.tenantId ?: "default"
        val endpointKey = requestContext.method + ":" + requestContext.uriInfo.path

        val limit = 100
        val windowSeconds = 60
        val result = rateLimiter.checkLimit(tenant, endpointKey, limit, windowSeconds)

        requestContext.headers.add("X-RateLimit-Limit", limit.toString())
        requestContext.headers.add("X-RateLimit-Remaining", result.remaining.toString())
        requestContext.headers.add("X-RateLimit-Reset", result.resetAtEpochSeconds.toString())

        if (!result.allowed) {
            metrics.markRateLimitExceeded(tenant)
            requestContext.abortWith(
                Response
                    .status(Response.Status.TOO_MANY_REQUESTS)
                    .entity(mapOf("code" to "RATE_LIMIT_EXCEEDED", "message" to "Too many requests"))
                    .build(),
            )
        }
    }
}
