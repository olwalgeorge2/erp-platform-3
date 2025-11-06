package com.erp.identity.infrastructure.service

import com.erp.identity.application.port.input.query.GetTenantQuery
import com.erp.identity.application.port.input.query.GetUserQuery
import com.erp.identity.application.port.input.query.ListTenantsQuery
import com.erp.identity.application.service.query.TenantQueryHandler
import com.erp.identity.application.service.query.UserQueryHandler
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.tenant.Tenant
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType

@ApplicationScoped
class IdentityQueryService(
    private val tenantQueryHandler: TenantQueryHandler,
    private val userQueryHandler: UserQueryHandler,
) {
    @Transactional(TxType.SUPPORTS)
    fun getTenant(query: GetTenantQuery): Tenant? = tenantQueryHandler.handle(query)

    @Transactional(TxType.SUPPORTS)
    fun listTenants(query: ListTenantsQuery): List<Tenant> = tenantQueryHandler.handle(query)

    @Transactional(TxType.SUPPORTS)
    fun getUser(query: GetUserQuery): User? = userQueryHandler.handle(query)
}
