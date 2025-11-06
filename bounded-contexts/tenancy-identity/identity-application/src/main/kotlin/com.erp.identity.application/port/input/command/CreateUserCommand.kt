package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateUserCommand(
    val tenantId: TenantId,
    @field:NotBlank
    @field:Size(min = 3, max = 50)
    val username: String,
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    @field:Size(min = 2, max = 200)
    val fullName: String,
    @field:NotBlank
    @field:Size(min = 8, max = 256)
    val password: String,
    val roleIds: Set<RoleId> = emptySet(),
    val metadata: Map<String, String> = emptyMap(),
    val createdBy: String? = null,
)
