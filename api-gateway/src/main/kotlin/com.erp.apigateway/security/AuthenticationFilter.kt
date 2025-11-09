package com.erp.apigateway.security

import com.erp.apigateway.config.PublicEndpointsConfig
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.jwt.JsonWebToken

@Provider
@Priority(Priorities.AUTHENTICATION)
class AuthenticationFilter : ContainerRequestFilter {
    @Inject
    lateinit var jwtValidator: JwtValidator

    @Inject
    lateinit var publicEndpointsConfig: PublicEndpointsConfig

    override fun filter(requestContext: ContainerRequestContext) {
        val path = requestContext.uriInfo.path
        if (publicEndpointsConfig.isPublic(path)) {
            return
        }

        val authHeader = requestContext.headers.getFirst("Authorization") ?: ""
        if (!authHeader.startsWith("Bearer ")) {
            abort(requestContext, "Missing or invalid Authorization header")
            return
        }

        val token = authHeader.removePrefix("Bearer ").trim()
        val jwt: JsonWebToken =
            try {
                jwtValidator.parse(token)
            } catch (e: Exception) {
                abort(requestContext, "Invalid token")
                return
            }

        val roles =
            when {
                jwt.claimNames.contains("roles") -> jwt.getClaim<Collection<String>>("roles").toSet()
                jwt.groups != null -> jwt.groups
                else -> emptySet()
            }

        val isSecure = requestContext.securityContext.isSecure
        val principal = jwt.name
        requestContext.securityContext = GatewaySecurityContext(principal, roles, isSecure)
    }

    private fun abort(
        ctx: ContainerRequestContext,
        message: String,
    ) {
        ctx.abortWith(
            Response
                .status(Response.Status.UNAUTHORIZED)
                .entity(mapOf("code" to "UNAUTHORIZED", "message" to message))
                .build(),
        )
    }
}
