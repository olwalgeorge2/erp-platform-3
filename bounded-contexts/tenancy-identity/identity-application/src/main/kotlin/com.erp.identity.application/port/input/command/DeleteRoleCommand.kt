package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
import jakarta.validation.constraints.NotNull

data class DeleteRoleCommand(
    @field:NotNull(message = "Tenant ID is required")
    val tenantId: TenantId,
    @field:NotNull(message = "Role ID is required")
    val roleId: RoleId,
    val requestedBy: String? = null,
)
