package com.erp.apigateway.health

import com.erp.apigateway.metrics.GatewayMetrics
import io.quarkus.redis.datasource.RedisDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import org.slf4j.LoggerFactory

/**
 * Redis readiness health check.
 *
 * Verifies that the Redis connection is available before marking the service as ready.
 * If Redis is down, the gateway will fail readiness checks and be removed from load balancers.
 *
 * This check runs on every readiness probe invocation (typically every 5-10 seconds).
 */
@Readiness
@ApplicationScoped
class RedisReadinessCheck
    @Inject
    constructor(
        private val redisDataSource: RedisDataSource,
        private val metrics: GatewayMetrics,
    ) : HealthCheck {
        private val logger = LoggerFactory.getLogger(RedisReadinessCheck::class.java)

        override fun call(): HealthCheckResponse =
            try {
                // Ping Redis to verify connectivity
                val commands = redisDataSource.value(String::class.java)
                val startTime = System.currentTimeMillis()

                // Perform a lightweight operation (GET on a non-existent key is faster than PING)
                commands.get("health:check:probe")

                val latencyMs = System.currentTimeMillis() - startTime

                logger.debug("Redis health check passed ({}ms)", latencyMs)
                metrics.setRedisHealth(true)
                metrics.setRedisLatencyMs(latencyMs)

                HealthCheckResponse
                    .builder()
                    .name("Redis connectivity")
                    .up()
                    .withData("latency_ms", latencyMs)
                    .withData("connection", "active")
                    .build()
            } catch (e: Exception) {
                logger.error("Redis health check failed: {}", e.message, e)
                metrics.setRedisHealth(false)

                HealthCheckResponse
                    .builder()
                    .name("Redis connectivity")
                    .down()
                    .withData("error", e.message ?: "Unknown error")
                    .withData("error_type", e.javaClass.simpleName)
                    .build()
            }
    }
