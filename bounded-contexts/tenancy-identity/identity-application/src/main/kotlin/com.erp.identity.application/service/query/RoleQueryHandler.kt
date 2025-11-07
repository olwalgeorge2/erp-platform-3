package com.erp.identity.application.service.query

import com.erp.identity.application.port.input.query.ListRolesQuery
import com.erp.identity.application.port.output.RoleRepository
import com.erp.identity.domain.model.identity.Role
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.shared.types.results.Result

class RoleQueryHandler(
    private val roleRepository: RoleRepository,
) {
    fun list(query: ListRolesQuery): Result<List<Role>> =
        roleRepository.list(
            tenantId = query.tenantId,
            limit = query.limit,
            offset = query.offset,
        )

    fun get(
        tenantId: TenantId,
        roleId: RoleId,
    ): Result<Role?> = roleRepository.findById(tenantId, roleId)
}
