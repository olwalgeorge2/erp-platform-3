package com.erp.identity.infrastructure.persistence

import com.erp.identity.application.port.output.RoleRepository
import com.erp.identity.domain.model.identity.Role
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.infrastructure.persistence.entity.RoleEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaRoleRepository(
    private val entityManager: EntityManager,
) : RoleRepository {
    override fun findById(
        tenantId: TenantId,
        roleId: RoleId,
    ): Role? =
        entityManager
            .find(RoleEntity::class.java, roleId.value)
            ?.takeIf { it.tenantId == tenantId.value }
            ?.toDomain()

    override fun findByIds(
        tenantId: TenantId,
        roleIds: Set<RoleId>,
    ): List<Role> {
        if (roleIds.isEmpty()) {
            return emptyList()
        }

        val ids = roleIds.map(RoleId::value).toSet()

        val query =
            entityManager.createQuery(
                """
                SELECT r FROM RoleEntity r 
                WHERE r.tenantId = :tenantId AND r.id IN :ids
                """.trimIndent(),
                RoleEntity::class.java,
            )
        query.setParameter("tenantId", tenantId.value)
        query.setParameter("ids", ids)

        return query.resultList.map(RoleEntity::toDomain)
    }

    fun save(role: Role): Role {
        val existing = entityManager.find(RoleEntity::class.java, role.id.value)
        val managed =
            if (existing != null) {
                existing.permissions = role.permissions.map { com.erp.identity.infrastructure.persistence.entity.PermissionEmbeddable.from(it) }.toMutableSet()
                existing.description = role.description
                existing.isSystem = role.isSystem
                existing.metadata = role.metadata.toMutableMap()
                existing.updatedAt = role.updatedAt
                entityManager.merge(existing)
            } else {
                entityManager.merge(RoleEntity.from(role))
            }
        entityManager.flush()
        return managed.toDomain()
    }
}
