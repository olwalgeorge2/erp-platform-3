package com.erp.identity.application.port.output

import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.shared.types.results.Result

interface UserRepository {
    fun findById(
        tenantId: TenantId,
        userId: UserId,
    ): Result<User?>

    fun findByUsername(
        tenantId: TenantId,
        username: String,
    ): Result<User?>

    fun findByEmail(
        tenantId: TenantId,
        email: String,
    ): Result<User?>

    fun existsByUsername(
        tenantId: TenantId,
        username: String,
    ): Result<Boolean>

    fun existsByEmail(
        tenantId: TenantId,
        email: String,
    ): Result<Boolean>

    fun save(user: User): Result<User>
}
