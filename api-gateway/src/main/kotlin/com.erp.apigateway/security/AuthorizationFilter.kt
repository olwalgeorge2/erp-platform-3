package com.erp.apigateway.security

import com.erp.apigateway.metrics.GatewayMetrics
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.Config

@Provider
@Priority(Priorities.AUTHORIZATION)
class AuthorizationFilter : ContainerRequestFilter {
    @Inject
    lateinit var config: Config

    @Inject
    lateinit var metrics: GatewayMetrics

    override fun filter(requestContext: ContainerRequestContext) {
        val protectedPrefixes: List<String> =
            try {
                config.getValues("gateway.auth.protected-prefixes", String::class.java)
            } catch (_: Exception) {
                emptyList()
            }
        if (protectedPrefixes.isEmpty()) return

        val path = requestContext.uriInfo.path.let { if (it.startsWith("/")) it else "/$it" }
        val isProtected =
            protectedPrefixes.any { pref ->
                val p = if (pref.startsWith("/")) pref else "/$pref"
                val normalized = if (p.endsWith("/")) p else "$p/"
                path.startsWith(normalized)
            }
        if (!isProtected) return

        // If we are here, AuthenticationFilter should have already allowed the request
        val roles =
            requestContext.securityContext.userPrincipal?.let {
                (requestContext.securityContext as? GatewaySecurityContext)?.roles()
            }
                ?: emptySet()
        if (roles.isEmpty()) {
            metrics.markAuthFailure("forbidden")
            requestContext.abortWith(
                Response
                    .status(Response.Status.FORBIDDEN)
                    .entity(mapOf("code" to "FORBIDDEN", "message" to "Insufficient permissions"))
                    .build(),
            )
        }
    }
}
