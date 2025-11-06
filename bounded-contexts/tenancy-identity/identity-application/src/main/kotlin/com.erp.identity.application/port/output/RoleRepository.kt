package com.erp.identity.application.port.output

import com.erp.identity.domain.model.identity.Role
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId

interface RoleRepository {
    fun findById(
        tenantId: TenantId,
        roleId: RoleId,
    ): Role?

    fun findByIds(
        tenantId: TenantId,
        roleIds: Set<RoleId>,
    ): List<Role>
}
