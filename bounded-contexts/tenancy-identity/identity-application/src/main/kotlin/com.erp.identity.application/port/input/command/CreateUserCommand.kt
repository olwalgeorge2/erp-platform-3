package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId

data class CreateUserCommand(
    val tenantId: TenantId,
    val username: String,
    val email: String,
    val fullName: String,
    val password: String,
    val roleIds: Set<RoleId> = emptySet(),
    val metadata: Map<String, String> = emptyMap(),
    val createdBy: String? = null,
)
