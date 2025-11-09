package com.erp.apigateway.proxy

import com.erp.apigateway.routing.ServiceRoute
import com.erp.apigateway.routing.ServiceTarget
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import jakarta.ws.rs.core.MultivaluedHashMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ProxyServiceWireMockTest {
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

    @Test
    fun forwardsGetAndCopiesStatusAndBody() {
        wireMock.stubFor(
            get(urlEqualTo("/api/v1/identity/ping?x=1"))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                        .withHeader("X-Upstream", "true")
                        .withBody("pong"),
                ),
        )

        val route =
            ServiceRoute(
                pattern = "/api/v1/identity/*",
                target = ServiceTarget(baseUrl = "http://localhost:${wireMock.runtimeInfo.httpPort}"),
                authRequired = false,
            )
        val headers = MultivaluedHashMap<String, String>()
        val query = MultivaluedHashMap<String, String>()
        query.add("x", "1")

        val resp = service.forwardGet(route, "/api/v1/identity/ping", query, headers)

        assertEquals(201, resp.status)
        assertEquals("pong", String(resp.entity as ByteArray))
        // Upstream header should be preserved
        assertEquals("true", resp.getHeaderString("X-Upstream"))
    }
}
