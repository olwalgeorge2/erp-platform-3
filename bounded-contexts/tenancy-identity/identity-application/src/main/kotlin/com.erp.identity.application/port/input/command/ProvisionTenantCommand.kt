package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.tenant.Organization
import com.erp.identity.domain.model.tenant.Subscription
import java.util.UUID

data class ProvisionTenantCommand(
    val name: String,
    val slug: String,
    val subscription: Subscription,
    val organization: Organization?,
    val metadata: Map<String, String> = emptyMap(),
    val requestedBy: UUID? = null,
)
