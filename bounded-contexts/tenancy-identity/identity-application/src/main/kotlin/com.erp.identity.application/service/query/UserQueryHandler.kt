package com.erp.identity.application.service.query

import com.erp.identity.application.port.input.query.GetUserQuery
import com.erp.identity.application.port.output.UserRepository
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.shared.types.results.Result

class UserQueryHandler(
    private val userRepository: UserRepository,
) {
    fun handle(query: GetUserQuery): Result<User?> = userRepository.findById(query.tenantId, query.userId)

    fun findByUsername(
        tenantId: TenantId,
        username: String,
    ): Result<User?> = userRepository.findByUsername(tenantId, username)

    fun findByEmail(
        tenantId: TenantId,
        email: String,
    ): Result<User?> = userRepository.findByEmail(tenantId, email)
}
