package com.erp.apigateway.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

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

    // Backend health (0/1) per service
    private val backendUp: MutableMap<String, AtomicInteger> = ConcurrentHashMap()

    fun setBackendHealth(
        service: String,
        up: Boolean,
    ) {
        val gauge =
            backendUp.computeIfAbsent(service) {
                val ref = AtomicInteger(0)
                registry.gauge("gateway_backend_up", Tags.of("service", service), ref)
                ref
            }
        gauge.set(if (up) 1 else 0)
    }

    // Redis health (0/1)
    private val redisUpRef = AtomicInteger(0)
    private val redisLatencyRef = AtomicLong(0)

    init {
        registry.gauge("gateway_redis_up", redisUpRef)
        registry.gauge("gateway_redis_latency_ms", redisLatencyRef)
    }

    fun setRedisHealth(up: Boolean) {
        redisUpRef.set(if (up) 1 else 0)
    }

    fun setRedisLatencyMs(latencyMs: Long) {
        redisLatencyRef.set(latencyMs)
    }

    // Proxy retries per route
    fun markRetry(routeKey: String) {
        registry.counter("gateway_proxy_retries_total", Tags.of("route", routeKey)).increment()
    }

    // Circuit breaker metrics per route
    private val circuitState: MutableMap<String, AtomicInteger> = ConcurrentHashMap()

    fun setCircuitOpen(
        routeKey: String,
        open: Boolean,
    ) {
        val ref =
            circuitState.computeIfAbsent(routeKey) {
                val r = AtomicInteger(0)
                registry.gauge("gateway_circuit_state", Tags.of("route", routeKey), r)
                r
            }
        ref.set(if (open) 1 else 0)
        if (open) {
            registry.counter("gateway_circuit_open_total", Tags.of("route", routeKey)).increment()
        }
    }
}
