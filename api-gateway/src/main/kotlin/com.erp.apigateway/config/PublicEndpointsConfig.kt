package com.erp.apigateway.config

import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.Config

@ApplicationScoped
class PublicEndpointsConfig {
    // Built-in public prefixes
    private val defaults =
        mutableListOf(
            "/q/health/", // SmallRye Health endpoints
            "/health/", // Legacy health endpoint (deprecated)
            "/q/metrics", // Prometheus metrics
            "/metrics", // Legacy metrics endpoint
            "/api/v1/identity/auth/", // Identity authentication endpoints
        )

    @Inject
    lateinit var config: Config

    @PostConstruct
    fun loadAdditionalPrefixes() {
        // Optional: gateway.public-prefixes can be comma-separated or array in YAML
        val additional: List<String> =
            try {
                config.getValues("gateway.public-prefixes", String::class.java)
            } catch (_: Exception) {
                emptyList()
            }
        additional
            .map { if (it.startsWith("/")) it else "/$it" }
            .forEach { defaults.add(if (it.endsWith("/")) it else "$it/") }
    }

    fun isPublic(path: String): Boolean {
        val normalized = if (path.startsWith("/")) path else "/$path"
        val withSlash = if (normalized.endsWith("/")) normalized else "$normalized/"
        return defaults.any { normalized.startsWith(it) || withSlash.startsWith(it) }
    }
}
