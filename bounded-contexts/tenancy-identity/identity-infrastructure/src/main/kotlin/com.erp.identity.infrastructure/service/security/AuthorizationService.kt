package com.erp.identity.infrastructure.service.security

import com.erp.identity.domain.model.identity.PermissionScope
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.infrastructure.adapter.input.rest.ErrorResponse
import com.erp.identity.infrastructure.web.RequestPrincipalContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response

@ApplicationScoped
class AuthorizationService {
    fun requireRoleRead(tenantId: TenantId): Response? =
        authorize(
            tenantId = tenantId,
            requiredPermission = PermissionDescriptor(resource = "roles", action = "read"),
        )

    fun requireRoleManagement(tenantId: TenantId): Response? =
        authorize(
            tenantId = tenantId,
            requiredPermission = PermissionDescriptor(resource = "roles", action = "manage"),
        )

    private fun authorize(
        tenantId: TenantId,
        requiredPermission: PermissionDescriptor,
    ): Response? {
        val principal =
            RequestPrincipalContext.get()
                ?: return forbidden(
                    code = "AUTHENTICATION_REQUIRED",
                    message = "Authentication is required to access this resource.",
                )

        val principalTenantId = principal.tenantId
        val tenantValue = tenantId.value.toString()
        if (!principal.hasRole("SYSTEM_ADMIN") &&
            !principalTenantId.isNullOrBlank() &&
            !principalTenantId.equals(tenantValue, ignoreCase = true)
        ) {
            return forbidden(
                code = "TENANT_ACCESS_DENIED",
                message = "User is not allowed to manage the requested tenant.",
                details = mapOf("tenantId" to tenantValue),
            )
        }

        val hasPrivilege =
            principal.hasRole("SYSTEM_ADMIN") ||
                principal.hasRole("TENANT_ADMIN") ||
                principal.hasPermission(requiredPermission.resource, requiredPermission.action)
        if (!hasPrivilege) {
            return forbidden(
                code = "ACCESS_DENIED",
                message = "Missing permission ${requiredPermission.resource}:${requiredPermission.action}",
            )
        }

        return null
    }

    private fun forbidden(
        code: String,
        message: String,
        details: Map<String, String> = emptyMap(),
    ): Response =
        Response
            .status(Response.Status.FORBIDDEN)
            .entity(
                ErrorResponse(
                    code = code,
                    message = message,
                    details = details,
                ),
            ).build()
}

data class PermissionDescriptor(
    val resource: String,
    val action: String,
    val scope: PermissionScope = PermissionScope.TENANT,
)
