package com.erp.apigateway.ratelimit

import com.erp.apigateway.infrastructure.RedisService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class RateLimiter {
    @Inject
    lateinit var redisService: RedisService

    fun checkLimit(
        tenantId: String,
        endpointKey: String,
        limit: Int,
        windowSeconds: Int,
    ): RateLimitResult {
        val windowStart = System.currentTimeMillis() / 1000 / windowSeconds * windowSeconds
        val key = "ratelimit:$tenantId:$endpointKey:$windowStart"
        val current = redisService.incr(key)
        if (current == 1L) {
            redisService.expire(key, windowSeconds.toLong())
        }
        val remaining = (limit - current).toInt()
        val resetAt = windowStart + windowSeconds
        return RateLimitResult(
            allowed = current <= limit,
            remaining = remaining.coerceAtLeast(0),
            resetAtEpochSeconds = resetAt,
        )
    }
}
