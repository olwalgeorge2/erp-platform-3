package com.erp.identity.application.service.query

import com.erp.identity.application.port.input.query.GetTenantQuery
import com.erp.identity.application.port.input.query.ListTenantsQuery
import com.erp.identity.application.port.output.TenantRepository
import com.erp.identity.domain.model.tenant.Tenant

class TenantQueryHandler(
    private val tenantRepository: TenantRepository,
) {
    fun handle(query: GetTenantQuery): Tenant? = tenantRepository.findById(query.tenantId)

    fun handle(query: ListTenantsQuery): List<Tenant> =
        tenantRepository.listTenants(
            status = query.status,
            limit = query.limit,
            offset = query.offset,
        )
}
