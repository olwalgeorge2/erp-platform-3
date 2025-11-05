package com.erp.identity.application.port.input.query

import com.erp.identity.domain.model.tenant.TenantId

data class GetTenantQuery(
    val tenantId: TenantId,
)
