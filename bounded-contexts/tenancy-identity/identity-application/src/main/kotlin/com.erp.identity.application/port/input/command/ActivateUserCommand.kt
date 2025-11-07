package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId
import jakarta.validation.constraints.NotNull

data class ActivateUserCommand(
    @field:NotNull(message = "Tenant ID is required")
    val tenantId: TenantId,
    @field:NotNull(message = "User ID is required")
    val userId: UserId,
    val requestedBy: String? = null,
)
