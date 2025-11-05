package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.tenant.TenantId

data class AuthenticateUserCommand(
    val tenantId: TenantId,
    val usernameOrEmail: String,
    val password: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)
