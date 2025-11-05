package com.erp.identity.application.port.output

import com.erp.identity.domain.model.tenant.Tenant
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.model.tenant.TenantStatus

interface TenantRepository {
    fun findById(tenantId: TenantId): Tenant?

    fun findBySlug(slug: String): Tenant?

    fun existsBySlug(slug: String): Boolean

    fun save(tenant: Tenant): Tenant

    fun listTenants(
        status: TenantStatus?,
        limit: Int,
        offset: Int,
    ): List<Tenant>
}
