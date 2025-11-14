package com.erp.apigateway.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import io.smallrye.config.WithName
import java.util.Optional

@ConfigMapping(prefix = "gateway.auth")
interface GatewayAuthConfig {
    @WithName("protected-prefixes")
    @WithDefault("")
    fun protectedPrefixes(): List<String>

    @WithName("scope-rules")
    fun scopeRules(): Optional<List<ScopeRule>>

    interface ScopeRule {
        fun prefix(): String

        @WithName("any-role")
        fun anyRole(): List<String>
    }
}
