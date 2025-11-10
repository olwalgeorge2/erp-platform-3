package com.erp.apigateway.health

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Liveness
import org.slf4j.LoggerFactory

/**
 * Gateway liveness health check.
 *
 * Verifies that the gateway application is alive and responsive.
 * Unlike readiness checks, liveness failures indicate the process should be restarted.
 *
 * This check validates:
 * - JVM memory is within acceptable limits (not OOM)
 * - Application threads are not deadlocked
 *
 * This check runs on every liveness probe invocation (typically every 10-30 seconds).
 */
@Liveness
@ApplicationScoped
class GatewayLivenessCheck : HealthCheck {
    private val logger = LoggerFactory.getLogger(GatewayLivenessCheck::class.java)

    // Memory threshold: 95% heap usage indicates potential OOM
    private val heapUsageThreshold = 0.95

    override fun call(): HealthCheckResponse {
        val builder =
            HealthCheckResponse
                .builder()
                .name("Gateway liveness")

        return try {
            val memoryCheck = checkMemory()
            val threadCheck = checkThreads()

            val healthy = memoryCheck.healthy && threadCheck.healthy

            if (healthy) {
                builder.up()
                logger.debug("Gateway liveness check passed")
            } else {
                builder.down()
                logger.error(
                    "Gateway liveness check failed - Memory: {}, Threads: {}",
                    memoryCheck.status,
                    threadCheck.status,
                )
            }

            // Add diagnostic data
            builder
                .withData("heap_used_mb", memoryCheck.heapUsedMb)
                .withData("heap_max_mb", memoryCheck.heapMaxMb)
                .withData("heap_usage_percent", memoryCheck.heapUsagePercent.toLong())
                .withData("thread_count", threadCheck.threadCount.toLong())
                .withData("deadlocked_threads", threadCheck.deadlockedThreads.toLong())
                .withData("memory_status", memoryCheck.status)
                .withData("thread_status", threadCheck.status)

            builder.build()
        } catch (e: Exception) {
            logger.error("Gateway liveness check error: {}", e.message, e)
            builder
                .down()
                .withData("error", e.message ?: "Unknown error")
                .build()
        }
    }

    private fun checkMemory(): MemoryStatus {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory

        val heapUsage = usedMemory.toDouble() / maxMemory.toDouble()

        val heapUsedMb = usedMemory / 1024 / 1024
        val heapMaxMb = maxMemory / 1024 / 1024
        val heapUsagePercent = (heapUsage * 100).toInt()

        return if (heapUsage < heapUsageThreshold) {
            MemoryStatus(
                healthy = true,
                status = "OK",
                heapUsedMb = heapUsedMb,
                heapMaxMb = heapMaxMb,
                heapUsagePercent = heapUsagePercent,
            )
        } else {
            MemoryStatus(
                healthy = false,
                status = "CRITICAL - Heap usage above ${(heapUsageThreshold * 100).toInt()}%",
                heapUsedMb = heapUsedMb,
                heapMaxMb = heapMaxMb,
                heapUsagePercent = heapUsagePercent,
            )
        }
    }

    private fun checkThreads(): ThreadStatus {
        val threadMXBean =
            java.lang.management.ManagementFactory
                .getThreadMXBean()
        val threadCount = threadMXBean.threadCount
        val deadlockedThreads = threadMXBean.findDeadlockedThreads()?.size ?: 0

        return if (deadlockedThreads == 0) {
            ThreadStatus(
                healthy = true,
                status = "OK",
                threadCount = threadCount,
                deadlockedThreads = deadlockedThreads,
            )
        } else {
            ThreadStatus(
                healthy = false,
                status = "CRITICAL - $deadlockedThreads deadlocked threads detected",
                threadCount = threadCount,
                deadlockedThreads = deadlockedThreads,
            )
        }
    }

    private data class MemoryStatus(
        val healthy: Boolean,
        val status: String,
        val heapUsedMb: Long,
        val heapMaxMb: Long,
        val heapUsagePercent: Int,
    )

    private data class ThreadStatus(
        val healthy: Boolean,
        val status: String,
        val threadCount: Int,
        val deadlockedThreads: Int,
    )
}
