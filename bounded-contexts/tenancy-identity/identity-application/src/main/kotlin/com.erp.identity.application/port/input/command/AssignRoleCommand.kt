package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId
import jakarta.validation.constraints.NotNull

data class AssignRoleCommand(
    @field:NotNull(message = "Tenant ID is required")
    val tenantId: TenantId,
    @field:NotNull(message = "User ID is required")
    val userId: UserId,
    @field:NotNull(message = "Role ID is required")
    val roleId: RoleId,
    val assignedBy: String? = null,
)
