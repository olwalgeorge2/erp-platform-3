package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.tenant.TenantId
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SuspendTenantCommand(
    @field:NotNull(message = "Tenant ID is required")
    val tenantId: TenantId,
    @field:NotBlank(message = "Suspension reason is required")
    val reason: String,
    val requestedBy: String? = null,
)
