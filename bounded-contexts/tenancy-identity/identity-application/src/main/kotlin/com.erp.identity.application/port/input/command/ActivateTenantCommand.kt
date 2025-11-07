package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.tenant.TenantId
import jakarta.validation.constraints.NotNull

data class ActivateTenantCommand(
    @field:NotNull(message = "Tenant ID is required")
    val tenantId: TenantId,
    val requestedBy: String? = null,
)
