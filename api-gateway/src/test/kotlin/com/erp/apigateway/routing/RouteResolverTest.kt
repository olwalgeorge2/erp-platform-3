package com.erp.apigateway.routing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RouteResolverTest {
    private val resolver =
        RouteResolver(
            routes =
                listOf(
                    ServiceRoute(
                        pattern = "/api/v1/identity/*",
                        target = ServiceTarget(baseUrl = "http://localhost:8081"),
                        authRequired = false,
                    ),
                ),
        )

    @Test
    fun resolvesIdentityWildcard() {
        val route = resolver.resolve("/api/v1/identity/users/me")
        assertEquals("/api/v1/identity/*", route.pattern)
        assertEquals("http://localhost:8081", route.target.baseUrl)
    }

    @Test
    fun throwsForUnknownPath() {
        assertThrows(RouteNotFoundException::class.java) {
            resolver.resolve("/api/v1/unknown/resource")
        }
    }
}
