package com.erp.apigateway.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import java.time.Duration

@ConfigMapping(prefix = "gateway")
interface GatewayRoutesConfig {
    fun routes(): List<RouteEntry>?

    fun auth(): Auth?

    @io.smallrye.config.WithName("public-prefixes")
    fun publicPrefixes(): List<String>?

    interface RouteEntry {
        fun pattern(): String

        fun baseUrl(): String

        @WithDefault("PT5S")
        fun timeout(): Duration

        @WithDefault("2")
        fun retries(): Int

        @WithDefault("true")
        fun authRequired(): Boolean

        fun rewrite(): Rewrite? // optional

        @WithDefault("/q/health/ready")
        fun healthPath(): String

        @WithDefault("100")
        fun backoffInitialMs(): Long

        @WithDefault("1000")
        fun backoffMaxMs(): Long

        @WithDefault("50")
        fun backoffJitterMs(): Long

        // Circuit breaker: open after consecutive failures, reset after interval
        @WithDefault("5")
        fun cbFailureThreshold(): Int

        @WithDefault("30000")
        fun cbResetMs(): Long
    }

    interface Rewrite {
        fun removePrefix(): String

        fun addPrefix(): String
    }
}

interface Auth {
    @WithDefault("100")
    fun minFailureDurationMs(): Long
}
