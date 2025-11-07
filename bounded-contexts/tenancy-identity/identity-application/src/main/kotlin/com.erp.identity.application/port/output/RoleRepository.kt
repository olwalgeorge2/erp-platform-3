package com.erp.identity.application.port.output

import com.erp.identity.domain.model.identity.Role
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.shared.types.results.Result

interface RoleRepository {
    fun findById(
        tenantId: TenantId,
        roleId: RoleId,
    ): Result<Role?>

    fun findByIds(
        tenantId: TenantId,
        roleIds: Set<RoleId>,
    ): Result<List<Role>>

    fun list(
        tenantId: TenantId,
        limit: Int,
        offset: Int,
    ): Result<List<Role>>

    fun existsByName(
        tenantId: TenantId,
        name: String,
    ): Result<Boolean>

    fun save(role: Role): Result<Role>

    fun delete(
        tenantId: TenantId,
        roleId: RoleId,
    ): Result<Unit>
}
