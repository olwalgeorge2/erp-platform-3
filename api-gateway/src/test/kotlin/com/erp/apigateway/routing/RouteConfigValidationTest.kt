package com.erp.apigateway.routing

import com.erp.apigateway.config.RouteConfiguration
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException

class RouteConfigValidationTest {
    @Test
    fun `rejects overlapping wildcard patterns`() {
        val producer = RouteConfiguration()
        val routes =
            listOf(
                ServiceRoute("/api/*", ServiceTarget("http://a"), authRequired = false),
                ServiceRoute("/api/v1/*", ServiceTarget("http://b"), authRequired = false),
            )
        val m = RouteConfiguration::class.java.getDeclaredMethod("validateRoutes", List::class.java)
        m.isAccessible = true
        val ex: InvocationTargetException =
            assertThrows(InvocationTargetException::class.java) {
                m.invoke(producer, routes)
            }
        assert(ex.targetException is IllegalArgumentException)
    }

    @Test
    fun `rejects non-absolute health path`() {
        val producer = RouteConfiguration()
        val routes =
            listOf(
                ServiceRoute(
                    "/api/*",
                    ServiceTarget(baseUrl = "http://a", healthPath = "q/ready"),
                    authRequired = false,
                ),
            )
        val m = RouteConfiguration::class.java.getDeclaredMethod("validateRoutes", List::class.java)
        m.isAccessible = true
        val ex: InvocationTargetException =
            assertThrows(InvocationTargetException::class.java) {
                m.invoke(producer, routes)
            }
        assert(ex.targetException is IllegalArgumentException)
    }
}
