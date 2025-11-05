package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId

data class UpdateCredentialsCommand(
    val tenantId: TenantId,
    val userId: UserId,
    val currentPassword: String? = null,
    val newPassword: String,
    val requestedBy: String? = null,
)
