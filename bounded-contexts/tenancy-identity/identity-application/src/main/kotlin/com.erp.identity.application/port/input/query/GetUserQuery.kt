package com.erp.identity.application.port.input.query

import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId

data class GetUserQuery(
    val tenantId: TenantId,
    val userId: UserId,
)
