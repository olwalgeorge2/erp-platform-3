package com.erp.identity.application.port.output

import com.erp.identity.domain.model.tenant.Tenant
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.model.tenant.TenantStatus
import com.erp.shared.types.results.Result

interface TenantRepository {
    fun findById(tenantId: TenantId): Result<Tenant?>

    fun findBySlug(slug: String): Result<Tenant?>

    fun existsBySlug(slug: String): Result<Boolean>

    fun save(tenant: Tenant): Result<Tenant>

    fun listTenants(
        status: TenantStatus?,
        limit: Int,
        offset: Int,
    ): Result<List<Tenant>>
}
