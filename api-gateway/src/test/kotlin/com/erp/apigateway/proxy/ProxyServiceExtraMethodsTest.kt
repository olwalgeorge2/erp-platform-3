package com.erp.apigateway.proxy

import com.erp.apigateway.metrics.GatewayMetrics
import com.erp.apigateway.routing.ServiceRoute
import com.erp.apigateway.routing.ServiceTarget
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.ws.rs.core.MultivaluedHashMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ProxyServiceExtraMethodsTest {
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
    }

    @Test
    fun `forwards DELETE without body`() {
        wireMock.stubFor(
            delete(urlEqualTo("/api/v1/identity/item/123"))
                .willReturn(aResponse().withStatus(204)),
        )

        val route =
            ServiceRoute(
                pattern = "/api/v1/identity/*",
                target = ServiceTarget(baseUrl = "http://localhost:${wireMock.runtimeInfo.httpPort}"),
                authRequired = false,
            )

        val headers = MultivaluedHashMap<String, String>()
        val query = MultivaluedHashMap<String, String>()

        val resp = service.forwardDelete(route, "/api/v1/identity/item/123", query, headers)
        assertEquals(204, resp.status)
    }

    @Test
    fun `forwards PATCH with body`() {
        wireMock.stubFor(
            patch(urlEqualTo("/api/v1/identity/item/123"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"patched\":true}"),
                ),
        )

        val route =
            ServiceRoute(
                pattern = "/api/v1/identity/*",
                target = ServiceTarget(baseUrl = "http://localhost:${wireMock.runtimeInfo.httpPort}"),
                authRequired = false,
            )

        val headers = MultivaluedHashMap<String, String>()
        headers.add("Content-Type", "application/json")
        val query = MultivaluedHashMap<String, String>()
        val body = "{}".toByteArray()

        val resp =
            service.forwardWithBody(
                route = route,
                method = "PATCH",
                incomingPath = "/api/v1/identity/item/123",
                queryParams = query,
                incomingHeaders = headers,
                body = body,
            )

        assertEquals(200, resp.status)
        assertEquals("{\"patched\":true}", String(resp.entity as ByteArray))
    }
}
