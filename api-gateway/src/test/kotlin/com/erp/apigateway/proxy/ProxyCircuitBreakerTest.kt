package com.erp.apigateway.proxy

import com.erp.apigateway.metrics.GatewayMetrics
import com.erp.apigateway.routing.ServiceRoute
import com.erp.apigateway.routing.ServiceTarget
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.ws.rs.core.MultivaluedHashMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ProxyCircuitBreakerTest {
    companion object {
        @JvmField
        @RegisterExtension
        val wireMock: WireMockExtension =
            WireMockExtension
                .newInstance()
                .options(WireMockConfiguration.wireMockConfig().dynamicPort())
                .build()
    }

    private val service = ProxyService()

    @BeforeEach
    fun setup() {
        val metrics = GatewayMetrics()
        metrics.registry = SimpleMeterRegistry()
        service.metrics = metrics
        wireMock.resetAll()
    }

    @Test
    fun `opens circuit after failures and short-circuits`() {
        // Always 500 from backend
        wireMock.stubFor(
            get(urlEqualTo("/api/v1/identity/down"))
                .willReturn(aResponse().withStatus(500)),
        )

        val route =
            ServiceRoute(
                pattern = "/api/v1/identity/*",
                target =
                    ServiceTarget(
                        baseUrl = "http://localhost:${wireMock.runtimeInfo.httpPort}",
                        timeoutSeconds = 2,
                        retries = 0,
                        cbFailureThreshold = 1,
                        cbResetMs = 60000,
                    ),
                authRequired = false,
            )

        val headers = MultivaluedHashMap<String, String>()
        val query = MultivaluedHashMap<String, String>()

        // First call attempts backend and returns 502 (mapped from exception/5xx)
        val r1 =
            service.forwardGet(
                route = route,
                incomingPath = "/api/v1/identity/down",
                queryParams = query,
                incomingHeaders = headers,
            )
        // Either 502 (proxy_exception) or 500 passthrough; accept 5xx
        assertEquals(true, r1.status in 500..599)

        // Next call should short-circuit to 503 due to open circuit
        val r2 =
            service.forwardGet(
                route = route,
                incomingPath = "/api/v1/identity/down",
                queryParams = query,
                incomingHeaders = headers,
            )
        assertEquals(503, r2.status)
    }
}
