package com.erp.identity.infrastructure.persistence

import com.erp.identity.application.port.output.UserRepository
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.infrastructure.persistence.entity.UserEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaUserRepository(
    private val entityManager: EntityManager,
) : UserRepository {
    override fun findById(
        tenantId: TenantId,
        userId: UserId,
    ): User? =
        entityManager
            .find(UserEntity::class.java, userId.value)
            ?.takeIf { it.tenantId == tenantId.value }
            ?.toDomain()

    override fun findByUsername(
        tenantId: TenantId,
        username: String,
    ): User? =
        entityManager
            .createQuery(
                """
                SELECT u FROM UserEntity u 
                WHERE u.tenantId = :tenantId AND LOWER(u.username) = LOWER(:username)
                """.trimIndent(),
                UserEntity::class.java,
            ).setParameter("tenantId", tenantId.value)
            .setParameter("username", username)
            .resultList
            .firstOrNull()
            ?.toDomain()

    override fun findByEmail(
        tenantId: TenantId,
        email: String,
    ): User? =
        entityManager
            .createQuery(
                """
                SELECT u FROM UserEntity u 
                WHERE u.tenantId = :tenantId AND LOWER(u.email) = LOWER(:email)
                """.trimIndent(),
                UserEntity::class.java,
            ).setParameter("tenantId", tenantId.value)
            .setParameter("email", email)
            .resultList
            .firstOrNull()
            ?.toDomain()

    override fun existsByUsername(
        tenantId: TenantId,
        username: String,
    ): Boolean =
        existsBy(
            tenantId = tenantId.value,
            field = "username",
            value = username,
        )

    override fun existsByEmail(
        tenantId: TenantId,
        email: String,
    ): Boolean =
        existsBy(
            tenantId = tenantId.value,
            field = "email",
            value = email,
        )

    override fun save(user: User): User {
        val existing = entityManager.find(UserEntity::class.java, user.id.value)
        val managed =
            if (existing != null) {
                existing.updateFrom(user)
                entityManager.merge(existing)
            } else {
                entityManager.merge(UserEntity.from(user))
            }
        entityManager.flush()
        return managed.toDomain()
    }

    private fun existsBy(
        tenantId: UUID,
        field: String,
        value: String,
    ): Boolean =
        entityManager
            .createQuery(
                """
                SELECT COUNT(u) FROM UserEntity u 
                WHERE u.tenantId = :tenantId AND LOWER(u.$field) = LOWER(:value)
                """.trimIndent(),
                java.lang.Long::class.java,
            ).setParameter("tenantId", tenantId)
            .setParameter("value", value)
            .singleResult > 0
}
