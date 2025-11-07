package com.erp.identity.infrastructure.service

import com.erp.identity.application.port.input.query.GetTenantQuery
import com.erp.identity.application.port.input.query.GetUserQuery
import com.erp.identity.application.port.input.query.ListTenantsQuery
import com.erp.identity.application.service.query.TenantQueryHandler
import com.erp.identity.application.service.query.UserQueryHandler
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.tenant.Tenant
import com.erp.shared.types.results.Result
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType

@ApplicationScoped
class IdentityQueryService(
    private val tenantQueryHandler: TenantQueryHandler,
    private val userQueryHandler: UserQueryHandler,
) {
    @Transactional(TxType.REQUIRED)
    fun getTenant(query: GetTenantQuery): Result<Tenant?> = tenantQueryHandler.handle(query)

    @Transactional(TxType.REQUIRED)
    fun listTenants(query: ListTenantsQuery): Result<List<Tenant>> = tenantQueryHandler.handle(query)

    @Transactional(TxType.REQUIRED)
    fun getUser(query: GetUserQuery): Result<User?> = userQueryHandler.handle(query)
}
