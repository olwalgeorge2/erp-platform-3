package com.erp.apigateway.config

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class PublicEndpointsConfig {
    // Simple prefix-based patterns considered public (no auth)
    private val defaults =
        listOf(
            "/q/health/", // SmallRye Health endpoints
            "/health/", // Legacy health endpoint (deprecated)
            "/q/metrics", // Prometheus metrics
            "/metrics", // Legacy metrics endpoint
            "/api/v1/identity/auth/", // Identity authentication endpoints
        )

    fun isPublic(path: String): Boolean {
        val normalized = if (path.startsWith("/")) path else "/$path"
        return defaults.any { normalized.startsWith(it) }
    }
}
