package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateUserCommand(
    @field:NotNull(message = "Tenant ID is required")
    val tenantId: TenantId,
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 50, message = "Username must be 3-50 characters long")
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "Username can contain letters, numbers, underscore, and hyphen only",
    )
    val username: String,
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,
    @field:NotBlank(message = "Full name is required")
    @field:Size(min = 2, max = 200, message = "Full name must be 2-200 characters long")
    val fullName: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 256, message = "Password must be 8-256 characters long")
    val password: String,
    val roleIds: Set<RoleId> = emptySet(),
    val metadata: Map<String, String> = emptyMap(),
    val createdBy: String? = null,
)
