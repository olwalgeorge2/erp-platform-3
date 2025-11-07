package com.erp.identity.application.port.input.query

import com.erp.identity.domain.model.tenant.TenantId

data class ListRolesQuery(
    val tenantId: TenantId,
    val limit: Int = 50,
    val offset: Int = 0,
)
