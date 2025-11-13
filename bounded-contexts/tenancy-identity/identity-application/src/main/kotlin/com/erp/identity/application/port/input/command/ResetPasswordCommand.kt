package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId
import jakarta.validation.constraints.NotBlank

data class ResetPasswordCommand(
    val tenantId: TenantId,
    val userId: UserId,
    @field:NotBlank
    val newPassword: String,
    val requirePasswordChange: Boolean = true,
    val requestedBy: String? = null,
)
