package com.erp.apigateway.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName
import java.time.Duration

@ConfigMapping(prefix = "gateway.rate-limits")
interface RateLimitsConfig {
    fun default(): LimitWindow

    fun overrides(): Overrides?

    interface Overrides {
        fun tenants(): Map<String, LimitWindow>?

        fun endpoints(): List<EndpointLimit>?
    }

    interface LimitWindow {
        @WithName("requests-per-minute")
        fun requestsPerMinute(): Int

        fun window(): Duration
    }

    interface EndpointLimit {
        fun pattern(): String

        @WithName("requests-per-minute")
        fun requestsPerMinute(): Int

        fun window(): Duration
    }
}
