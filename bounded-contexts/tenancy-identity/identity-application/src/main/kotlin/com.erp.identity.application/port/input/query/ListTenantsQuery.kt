package com.erp.identity.application.port.input.query

import com.erp.identity.domain.model.tenant.TenantStatus

data class ListTenantsQuery(
    val status: TenantStatus? = null,
    val limit: Int = 50,
    val offset: Int = 0,
) {
    init {
        require(limit in 1..200) { "Limit must be between 1 and 200" }
        require(offset >= 0) { "Offset cannot be negative" }
    }
}
