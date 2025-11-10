package com.erp.apigateway.proxy

import com.erp.apigateway.metrics.GatewayMetrics
import com.erp.apigateway.routing.ServiceRoute
import com.erp.apigateway.routing.ServiceTarget
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.ws.rs.core.MultivaluedHashMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ProxyServiceMethodsTest {
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
        // Initialize metrics with a simple meter registry
        val metrics = GatewayMetrics()
        metrics.registry = SimpleMeterRegistry()
        service.metrics = metrics
    }

    @Test
    fun `forwards POST with body and headers`() {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/identity/echo"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":true}"),
                ),
        )

        val route =
            ServiceRoute(
                pattern = "/api/v1/identity/*",
                target =
                    ServiceTarget(
                        baseUrl = "http://localhost:${wireMock.runtimeInfo.httpPort}",
                        timeoutSeconds = 5,
                        retries = 0,
                    ),
                authRequired = false,
            )

        val headers = MultivaluedHashMap<String, String>()
        headers.add("Content-Type", "application/json")
        val query = MultivaluedHashMap<String, String>()
        val body = "{\"msg\":\"hello\"}".toByteArray()

        val resp =
            service.forwardWithBody(
                route = route,
                method = "POST",
                incomingPath = "/api/v1/identity/echo",
                queryParams = query,
                incomingHeaders = headers,
                body = body,
            )

        assertEquals(200, resp.status)
        assertEquals("application/json", resp.getHeaderString("Content-Type"))
        assertEquals("{\"ok\":true}", String(resp.entity as ByteArray))
    }

    @Test
    fun `retries on 5xx then succeeds`() {
        // First 500 then 200
        wireMock.resetAll()
        wireMock.stubFor(
            put(urlEqualTo("/api/v1/identity/update"))
                .inScenario("retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("OK"),
        )
        wireMock.stubFor(
            put(urlEqualTo("/api/v1/identity/update"))
                .inScenario("retry")
                .whenScenarioStateIs("OK")
                .willReturn(aResponse().withStatus(204)),
        )

        val route =
            ServiceRoute(
                pattern = "/api/v1/identity/*",
                target =
                    ServiceTarget(
                        baseUrl = "http://localhost:${wireMock.runtimeInfo.httpPort}",
                        timeoutSeconds = 5,
                        retries = 1,
                    ),
                authRequired = false,
            )

        val headers = MultivaluedHashMap<String, String>()
        headers.add("Content-Type", "application/json")
        val query = MultivaluedHashMap<String, String>()
        val body = "{}".toByteArray()

        val resp =
            service.forwardWithBody(
                route = route,
                method = "PUT",
                incomingPath = "/api/v1/identity/update",
                queryParams = query,
                incomingHeaders = headers,
                body = body,
            )

        assertEquals(204, resp.status)
    }
}
