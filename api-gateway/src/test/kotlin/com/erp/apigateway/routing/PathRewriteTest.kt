package com.erp.apigateway.routing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PathRewriteTest {
    @Test
    fun `maps identity v1 prefix to api`() {
        val route =
            ServiceRoute(
                pattern = "/api/v1/identity/*",
                target = ServiceTarget(baseUrl = "http://localhost:8081"),
                authRequired = false,
                pathRewrite = PathRewrite(removePrefix = "/api/v1/identity", addPrefix = "/api"),
            )

        val mapped = route.mapUpstreamPath("/api/v1/identity/auth/login")
        assertEquals("/api/auth/login", mapped)
    }

    @Test
    fun `does not rewrite unrelated paths`() {
        val route =
            ServiceRoute(
                pattern = "/api/v1/identity/*",
                target = ServiceTarget(baseUrl = "http://localhost:8081"),
                authRequired = false,
                pathRewrite = PathRewrite(removePrefix = "/api/v1/identity", addPrefix = "/api"),
            )

        val mapped = route.mapUpstreamPath("/health")
        assertEquals("/health", mapped)
    }
}
