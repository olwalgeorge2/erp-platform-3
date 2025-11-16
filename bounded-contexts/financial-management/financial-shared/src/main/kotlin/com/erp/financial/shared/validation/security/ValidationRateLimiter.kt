package com.erp.financial.shared.validation.security

import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class RateLimitDecision(
    val allowed: Boolean,
    val remaining: Int,
    val resetEpochSeconds: Long,
    val bucket: String,
    val keyType: String,
    val key: String,
)

private data class RateLimitBucket(
    val name: String,
    val limit: Int,
    val windowSeconds: Long,
)

@ApplicationScoped
class ValidationRateLimiter(
    @ConfigProperty(name = "validation.security.rate-limit.default.limit", defaultValue = "100")
    private val defaultLimit: Int,
    @ConfigProperty(name = "validation.security.rate-limit.default.window-seconds", defaultValue = "60")
    private val defaultWindowSeconds: Long,
    @ConfigProperty(name = "validation.security.rate-limit.sox.limit", defaultValue = "20")
    private val soxLimit: Int,
    @ConfigProperty(name = "validation.security.rate-limit.sox.window-seconds", defaultValue = "60")
    private val soxWindowSeconds: Long,
    @ConfigProperty(name = "validation.security.rate-limit.user.limit", defaultValue = "500")
    private val userLimit: Int,
    @ConfigProperty(name = "validation.security.rate-limit.user.window-seconds", defaultValue = "60")
    private val userWindowSeconds: Long,
    @ConfigProperty(name = "validation.security.rate-limit.sox-path-prefixes", defaultValue = "")
    private val soxPathConfig: String,
    @ConfigProperty(name = "validation.security.abuse.threshold", defaultValue = "25")
    private val abuseThreshold: Int,
    @ConfigProperty(name = "validation.security.abuse.window-seconds", defaultValue = "60")
    private val abuseWindowSeconds: Long,
) {
    private val logger = Logger.getLogger(ValidationRateLimiter::class.java)
    private val ipBuckets =
        mapOf(
            "validation-default" to RateLimitBucket("validation-default", defaultLimit, defaultWindowSeconds),
            "validation-sox" to RateLimitBucket("validation-sox", soxLimit, soxWindowSeconds),
        )
    private val userBucket = RateLimitBucket("validation-user", userLimit, userWindowSeconds)

    private val rateCounters = ConcurrentHashMap<String, RateLimitState>()
    private val abuseCounters =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(abuseWindowSeconds))
            .build<String, AtomicInteger>()

    private val soxPathPrefixes =
        soxPathConfig.split(',').mapNotNull {
            val trimmed = it.trim()
            if (trimmed.isEmpty()) null else trimmed
        }

    fun determineBucket(path: String): RateLimitBucket =
        if (soxPathPrefixes.any { path.startsWith(it.trim()) }) {
            ipBuckets.getValue("validation-sox")
        } else {
            ipBuckets.getValue("validation-default")
        }

    fun checkIpLimit(ip: String, path: String): RateLimitDecision =
        checkLimit(
            bucket = determineBucket(path),
            keyType = "ip",
            rawKey = ip.ifBlank { "unknown" },
        )

    fun checkUserLimit(username: String?): RateLimitDecision? {
        val user = username?.takeIf { it.isNotBlank() } ?: return null
        return checkLimit(
            bucket = userBucket,
            keyType = "user",
            rawKey = user,
        )
    }

    fun bucketLimit(bucketName: String): Int =
        when (bucketName) {
            userBucket.name -> userBucket.limit
            else -> ipBuckets[bucketName]?.limit ?: defaultLimit
        }

    private fun checkLimit(
        bucket: RateLimitBucket,
        keyType: String,
        rawKey: String,
    ): RateLimitDecision {
        val windowSeconds = bucket.windowSeconds
        val nowSeconds = System.currentTimeMillis() / 1000
        val windowStart = nowSeconds / windowSeconds * windowSeconds
        val stateKey = "${bucket.name}:$keyType:$rawKey"
        val state =
            rateCounters.compute(stateKey) { _, existing ->
                val current =
                    existing?.takeIf { it.windowStart == windowStart }
                        ?: RateLimitState(windowStart, AtomicInteger(0))
                current.counter.incrementAndGet()
                current
            }!!
        val count = state.counter.get()
        val allowed = count <= bucket.limit
        if (!allowed) {
            registerAbuse(stateKey)
        }
        return RateLimitDecision(
            allowed = allowed,
            remaining = (bucket.limit - count).coerceAtLeast(0),
            resetEpochSeconds = windowStart + windowSeconds,
            bucket = bucket.name,
            keyType = keyType,
            key = rawKey,
        )
    }

    private fun registerAbuse(key: String) {
        val counter = abuseCounters.get(key) { AtomicInteger(0) }
        val current = counter.incrementAndGet()
        if (current == abuseThreshold) {
            logger.warnf("Validation abuse threshold exceeded key=%s threshold=%d window=%ds", key, abuseThreshold, abuseWindowSeconds)
        }
    }

    private data class RateLimitState(
        val windowStart: Long,
        val counter: AtomicInteger,
    )
}
