@file:Suppress(
    "ktlint:standard:chain-method-continuation",
    "ktlint:standard:argument-list-wrapping",
    "ktlint:standard:trailing-comma-on-call-site",
)

package com.erp.apigateway.routing

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager

class RouteWireMockResource : QuarkusTestResourceLifecycleManager {
    private lateinit var wm: WireMockServer

    override fun start(): Map<String, String> {
        wm = WireMockServer(WireMockConfiguration.options().dynamicPort())
        wm.start()

        // Upstream stubs
        wm.stubFor(
            WireMock.get(WireMock.urlEqualTo("/api/echo")).willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody("ok"),
            ),
        )
        wm.stubFor(
            WireMock.get(WireMock.urlEqualTo("/health")).willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody("UP"),
            ),
        )

        // Headers propagation/response filtering
        wm.stubFor(
            WireMock.get(WireMock.urlEqualTo("/api/headers"))
                .withHeader("X-Test-Header", WireMock.equalTo("foo"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Connection", "keep-alive")
                        .withHeader("Transfer-Encoding", "chunked")
                        .withHeader("X-Upstream", "bar")
                        .withBody("ok"),
                ),
        )

        // Tracing + Authorization headers pass-through
        wm.stubFor(
            WireMock.get(WireMock.urlEqualTo("/api/trace"))
                .withHeader("Authorization", WireMock.equalTo("Bearer testtoken"))
                .withHeader("traceparent", WireMock.matching("00-[0-9a-f]{32}-[0-9a-f]{16}-01"))
                .withHeader("tracestate", WireMock.equalTo("vendor=foo"))
                .willReturn(WireMock.aResponse().withStatus(200).withBody("trace-ok")),
        )

        val base = "http://localhost:${wm.port()}"
        return mapOf(
            // Route mapping with rewrite
            "gateway.routes[0].pattern" to "/api/v1/identity/*",
            "gateway.routes[0].base-url" to base,
            "gateway.routes[0].timeout" to "PT3S",
            "gateway.routes[0].retries" to "0",
            "gateway.routes[0].auth-required" to "false",
            "gateway.routes[0].health-path" to "/health",
            "gateway.routes[0].rewrite.remove-prefix" to "/api/v1/identity",
            "gateway.routes[0].rewrite.add-prefix" to "/api",
        )
    }

    override fun stop() {
        if (::wm.isInitialized) wm.stop()
    }
}
