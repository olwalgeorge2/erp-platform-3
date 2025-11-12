package com.erp.apigateway.filter

import com.erp.apigateway.config.RateLimitsConfig
import com.erp.apigateway.context.TenantContext
import com.erp.apigateway.infrastructure.RedisService
import com.erp.apigateway.metrics.GatewayMetrics
import com.erp.apigateway.ratelimit.RateLimiter
import jakarta.annotation.Priority
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.Config
import java.time.Duration

@Provider
@Priority(Priorities.AUTHENTICATION + 100)
class RateLimitFilter : ContainerRequestFilter {
    @Inject
    lateinit var tenantContext: TenantContext

    @Inject
    lateinit var rateLimiter: RateLimiter

    @Inject
    lateinit var metrics: GatewayMetrics

    @Inject
    lateinit var config: Config

    @Inject
    lateinit var rateLimitsConfig: Instance<RateLimitsConfig>

    @Inject
    lateinit var redis: RedisService

    override fun filter(requestContext: ContainerRequestContext) {
        try {
            val tenant = tenantContext.tenantId ?: "default"
            val endpointKey = requestContext.method + ":" + requestContext.uriInfo.path

            val (limit, windowSeconds) = resolveLimitAndWindow(tenant, requestContext.uriInfo.path)
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
        } catch (_: Exception) {
            // If Redis or configuration lookups fail, do not fail the request path.
            // Record an error metric and continue without enforcing a limit.
            metrics.markError("ratelimit_unavailable")
        }
    }

    private fun resolveLimitAndWindow(
        tenant: String,
        path: String,
    ): Pair<Int, Int> {
        // 1) Dynamic endpoint overrides (Redis)
        val dynEndpoint = resolveDynamicEndpointOverride(path)
        if (dynEndpoint != null) return dynEndpoint

        // 2) Dynamic tenant override (Redis)
        val dynTenant = resolveDynamicTenantOverride(tenant)
        if (dynTenant != null) return dynTenant

        // 3) Endpoint override (config, first match)
        val mapping = if (rateLimitsConfig.isResolvable) rateLimitsConfig.get() else null
        val endpointOverride =
            mapping
                ?.overrides()
                ?.endpoints()
                ?.firstOrNull { patternMatches(it.pattern(), path) }
        if (endpointOverride != null) {
            return Pair(endpointOverride.requestsPerMinute(), endpointOverride.window().seconds.toInt())
        }
        // 4) Tenant override (config)
        val tenantOverride = mapping?.overrides()?.tenants()?.get(tenant)
        if (tenantOverride != null) {
            return Pair(tenantOverride.requestsPerMinute(), tenantOverride.window().seconds.toInt())
        }
        // 5) Default
        if (mapping != null) {
            val def = mapping.default()
            return Pair(def.requestsPerMinute(), def.window().seconds.toInt())
        }
        // Fallback to legacy MP Config keys
        val limit = config.getValue("gateway.rate-limits.default.requests-per-minute", Int::class.java)
        val window: Duration = config.getValue("gateway.rate-limits.default.window", Duration::class.java)
        return Pair(limit, window.seconds.toInt())
    }

    private fun resolveDynamicTenantOverride(tenant: String): Pair<Int, Int>? =
        try {
            redis.get("rl:tenant:$tenant")?.let { parseOverride(it) }
        } catch (_: Exception) {
            null
        }

    private fun resolveDynamicEndpointOverride(path: String): Pair<Int, Int>? {
        return try {
            val keys = redis.keys("rl:endpoint:*")
            keys.forEach { k ->
                val pattern = k.removePrefix("rl:endpoint:")
                if (patternMatches(pattern, path)) {
                    val v = redis.get(k)
                    val p = parseOverride(v)
                    if (p != null) return p
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseOverride(value: String?): Pair<Int, Int>? {
        return try {
            if (value.isNullOrBlank()) return null
            val parts = value.split(":")
            if (parts.size != 2) return null
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (_: Exception) {
            null
        }
    }

    private fun patternMatches(
        pattern: String,
        path: String,
    ): Boolean {
        val normalized = if (path.startsWith("/")) path else "/$path"
        return if (pattern.endsWith("/*")) {
            val prefix = pattern.removeSuffix("/*")
            normalized.startsWith(prefix)
        } else {
            pattern == normalized
        }
    }
}
