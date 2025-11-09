package com.erp.apigateway.config

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class PublicEndpointsConfig {
    // Simple prefix-based patterns considered public (no auth)
    private val defaults =
        listOf(
            "/health/",
            "/metrics",
            "/api/v1/identity/auth/",
        )

    fun isPublic(path: String): Boolean {
        val normalized = if (path.startsWith("/")) path else "/$path"
        return defaults.any { normalized.startsWith(it) }
    }
}
