package com.erp.identity.application.service.query

import com.erp.identity.application.port.input.query.GetUserQuery
import com.erp.identity.application.port.output.UserRepository
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.tenant.TenantId

class UserQueryHandler(
    private val userRepository: UserRepository,
) {
    fun handle(query: GetUserQuery): User? = userRepository.findById(query.tenantId, query.userId)

    fun findByUsername(
        tenantId: TenantId,
        username: String,
    ): User? = userRepository.findByUsername(tenantId, username)

    fun findByEmail(
        tenantId: TenantId,
        email: String,
    ): User? = userRepository.findByEmail(tenantId, email)
}
