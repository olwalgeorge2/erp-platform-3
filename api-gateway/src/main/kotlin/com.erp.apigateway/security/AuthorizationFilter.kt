package com.erp.apigateway.security

import com.erp.apigateway.config.GatewayAuthConfig
import com.erp.apigateway.metrics.GatewayMetrics
import jakarta.annotation.Priority
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider

@Provider
@Priority(Priorities.AUTHORIZATION)
class AuthorizationFilter : ContainerRequestFilter {
    @Inject
    lateinit var authConfig: Instance<GatewayAuthConfig>

    @Inject
    lateinit var metrics: GatewayMetrics

    override fun filter(requestContext: ContainerRequestContext) {
        val config = if (authConfig.isResolvable) authConfig.get() else null
        val protectedPrefixes: List<String> = config?.protectedPrefixes() ?: emptyList()
        if (protectedPrefixes.isEmpty()) return

        val path = requestContext.uriInfo.path
        val isProtected = protectedPrefixes.any { matchesPrefix(path, it) }
        if (!isProtected) return

        val roles =
            requestContext.securityContext.userPrincipal?.let {
                (requestContext.securityContext as? GatewaySecurityContext)?.roles()
            }
                ?: emptySet()

        val scopeRules = config?.scopeRules()?.orElse(emptyList()) ?: emptyList()
        val matchedRule = scopeRules.firstOrNull { matchesPrefix(path, it.prefix()) }
        if (matchedRule != null) {
            val required =
                matchedRule
                    .anyRole()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .toSet()
            if (required.isNotEmpty() && roles.intersect(required).isEmpty()) {
                metrics.markAuthFailure("insufficient_scope")
                requestContext.abortWith(
                    Response
                        .status(Response.Status.FORBIDDEN)
                        .entity(
                            mapOf(
                                "code" to "FORBIDDEN",
                                "message" to "Missing required finance scope",
                            ),
                        ).build(),
                )
                return
            }
        } else if (roles.isEmpty()) {
            metrics.markAuthFailure("forbidden")
            requestContext.abortWith(
                Response
                    .status(Response.Status.FORBIDDEN)
                    .entity(mapOf("code" to "FORBIDDEN", "message" to "Insufficient permissions"))
                    .build(),
            )
        }
    }

    private fun matchesPrefix(
        path: String,
        prefix: String,
    ): Boolean {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val normalizedPrefix = if (prefix.startsWith("/")) prefix else "/$prefix"
        if (normalizedPath == normalizedPrefix) {
            return true
        }
        val prefixWithSlash = if (normalizedPrefix.endsWith("/")) normalizedPrefix else "$normalizedPrefix/"
        return normalizedPath.startsWith(prefixWithSlash)
    }
}
