package com.erp.apigateway.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import io.smallrye.config.WithName

@ConfigMapping(prefix = "gateway.auth")
interface GatewayAuthConfig {
    @WithName("protected-prefixes")
    @WithDefault("")
    fun protectedPrefixes(): List<String>
}
