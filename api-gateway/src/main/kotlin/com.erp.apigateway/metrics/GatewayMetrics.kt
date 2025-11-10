package com.erp.apigateway.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Duration

@ApplicationScoped
class GatewayMetrics {
    @Inject
    lateinit var registry: MeterRegistry

    fun recordRequest(
        method: String,
        endpoint: String,
        status: Int,
        durationMs: Long,
    ) {
        registry
            .counter(
                "gateway_requests_total",
                Tags.of("method", method, "endpoint", endpoint, "status", status.toString()),
            ).increment()

        registry
            .timer(
                "gateway_request_duration_seconds",
                Tags.of("method", method, "endpoint", endpoint, "status", status.toString()),
            ).record(Duration.ofMillis(durationMs))
    }

    fun markError(type: String) {
        registry.counter("gateway_errors_total", Tags.of("type", type)).increment()
    }

    fun markRateLimitExceeded(tenant: String) {
        registry.counter("gateway_ratelimit_exceeded_total", Tags.of("tenant", tenant)).increment()
    }

    fun markAuthFailure(reason: String) {
        registry.counter("gateway_auth_failures_total", Tags.of("reason", reason)).increment()
    }
}
