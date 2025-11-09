package com.erp.apigateway.ratelimit

data class RateLimitResult(
    val allowed: Boolean,
    val remaining: Int,
    val resetAtEpochSeconds: Long,
)
