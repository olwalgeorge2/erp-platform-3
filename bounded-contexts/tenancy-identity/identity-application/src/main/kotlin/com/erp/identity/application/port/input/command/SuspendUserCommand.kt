package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SuspendUserCommand(
    @field:NotNull
    val tenantId: TenantId,
    @field:NotNull
    val userId: UserId,
    @field:NotBlank
    val reason: String,
    val requestedBy: String? = null,
)
