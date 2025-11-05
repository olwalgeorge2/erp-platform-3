package com.erp.identity.application.port.output

import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId

interface UserRepository {
    fun findById(
        tenantId: TenantId,
        userId: UserId,
    ): User?

    fun findByUsername(
        tenantId: TenantId,
        username: String,
    ): User?

    fun findByEmail(
        tenantId: TenantId,
        email: String,
    ): User?

    fun existsByUsername(
        tenantId: TenantId,
        username: String,
    ): Boolean

    fun existsByEmail(
        tenantId: TenantId,
        email: String,
    ): Boolean

    fun save(user: User): User
}
