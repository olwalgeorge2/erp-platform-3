package com.erp.apigateway.security

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class AuthenticationTimingGuard(
    @ConfigProperty(name = "gateway.auth.min-failure-duration-ms", defaultValue = "100")
    private val minFailureMs: Long,
) {
    fun guard(startMillis: Long) {
        val elapsed = System.currentTimeMillis() - startMillis
        val remaining = minFailureMs - elapsed
        if (remaining > 0) {
            try {
                Thread.sleep(remaining)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
}
