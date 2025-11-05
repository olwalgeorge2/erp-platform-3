package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId

data class AssignRoleCommand(
    val tenantId: TenantId,
    val userId: UserId,
    val roleId: RoleId,
    val assignedBy: String? = null,
)
