package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.identity.Permission
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class UpdateRoleCommand(
    @field:NotNull(message = "Tenant ID is required")
    val tenantId: TenantId,
    @field:NotNull(message = "Role ID is required")
    val roleId: RoleId,
    @field:NotBlank(message = "Role name is required")
    @field:Size(min = 2, max = 100, message = "Role name must be 2-100 characters long")
    val name: String,
    @field:Size(max = 500, message = "Description must be 500 characters or fewer")
    val description: String,
    val permissions: Set<Permission> = emptySet(),
    val metadata: Map<String, String> = emptyMap(),
    val updatedBy: String? = null,
)
