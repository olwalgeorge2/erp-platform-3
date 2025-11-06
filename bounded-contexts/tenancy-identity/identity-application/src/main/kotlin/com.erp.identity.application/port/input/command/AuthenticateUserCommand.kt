package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.tenant.TenantId
import jakarta.validation.constraints.NotBlank

data class AuthenticateUserCommand(
    val tenantId: TenantId,
    @field:NotBlank
    val usernameOrEmail: String,
    @field:NotBlank
    val password: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)
