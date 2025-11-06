package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.tenant.Organization
import com.erp.identity.domain.model.tenant.Subscription
import java.util.UUID
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ProvisionTenantCommand(
    @field:NotBlank
    @field:Size(min = 2, max = 200)
    val name: String,
    @field:NotBlank
    @field:Size(min = 3, max = 50)
    @field:Pattern(
        regexp = "^[a-z0-9-]+$",
        message = "Slug can contain lowercase letters, numbers, and hyphen only",
    )
    val slug: String,
    val subscription: Subscription,
    val organization: Organization?,
    val metadata: Map<String, String> = emptyMap(),
    val requestedBy: UUID? = null,
)
