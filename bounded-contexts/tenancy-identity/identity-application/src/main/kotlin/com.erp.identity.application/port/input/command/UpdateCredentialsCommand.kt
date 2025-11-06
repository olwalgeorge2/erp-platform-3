package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateCredentialsCommand(
    val tenantId: TenantId,
    val userId: UserId,
    val currentPassword: String? = null,
    @field:NotBlank
    @field:Size(min = 8, max = 256)
    val newPassword: String,
    val requestedBy: String? = null,
)
