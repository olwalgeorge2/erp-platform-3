package com.erp.apigateway.security

import jakarta.ws.rs.core.SecurityContext
import java.security.Principal

class GatewaySecurityContext(
    private val principalName: String?,
    private val roles: Set<String>,
    private val isSecureRequest: Boolean,
) : SecurityContext {
    override fun getUserPrincipal(): Principal? = principalName?.let { SimplePrincipal(it) }

    override fun isUserInRole(role: String?): Boolean = role != null && roles.contains(role)

    override fun isSecure(): Boolean = isSecureRequest

    override fun getAuthenticationScheme(): String = "Bearer"

    private data class SimplePrincipal(
        val nameValue: String,
    ) : Principal {
        override fun getName(): String = nameValue
    }
}
