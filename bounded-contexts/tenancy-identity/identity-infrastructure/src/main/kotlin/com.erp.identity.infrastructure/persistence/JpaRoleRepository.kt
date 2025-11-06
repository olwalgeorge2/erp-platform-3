package com.erp.identity.infrastructure.persistence

import com.erp.identity.application.port.output.RoleRepository
import com.erp.identity.domain.model.identity.Role
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.infrastructure.persistence.entity.PermissionEmbeddable
import com.erp.identity.infrastructure.persistence.entity.RoleEntity
import com.erp.shared.types.results.Result
import com.erp.shared.types.results.Result.Companion.failure
import com.erp.shared.types.results.Result.Companion.success
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceException
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.jboss.logging.Logger

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaRoleRepository(
    private val entityManager: EntityManager,
) : RoleRepository {
    override fun findById(
        tenantId: TenantId,
        roleId: RoleId,
    ): Result<Role?> =
        runQuery("findById") {
            entityManager
                .find(RoleEntity::class.java, roleId.value)
                ?.takeIf { it.tenantId == tenantId.value }
                ?.toDomain()
        }

    override fun findByIds(
        tenantId: TenantId,
        roleIds: Set<RoleId>,
    ): Result<List<Role>> =
        runQuery("findByIds") {
            if (roleIds.isEmpty()) {
                emptyList()
            } else {
                val ids = roleIds.map(RoleId::value).toSet()
                entityManager
                    .createQuery(
                        """
                        SELECT r FROM RoleEntity r
                        WHERE r.tenantId = :tenantId AND r.id IN :ids
                        """.trimIndent(),
                        RoleEntity::class.java,
                    ).setParameter("tenantId", tenantId.value)
                    .setParameter("ids", ids)
                    .resultList
                    .map(RoleEntity::toDomain)
            }
        }

    fun save(role: Role): Result<Role> =
        runQuery("save") {
            val existing = entityManager.find(RoleEntity::class.java, role.id.value)
            val managed =
                if (existing != null) {
                    existing.permissions = role.permissions.map { PermissionEmbeddable.from(it) }.toMutableSet()
                    existing.description = role.description
                    existing.isSystem = role.isSystem
                    existing.metadata = role.metadata.toMutableMap()
                    existing.updatedAt = role.updatedAt
                    entityManager.merge(existing)
                } else {
                    entityManager.merge(RoleEntity.from(role))
                }
            entityManager.flush()
            managed.toDomain()
        }

    private fun <T> runQuery(
        operation: String,
        block: () -> T,
    ): Result<T> =
        try {
            success(block())
        } catch (ex: PersistenceException) {
            LOGGER.errorf(ex, "RoleRepository.%s failed", operation)
            failure(
                code = "ROLE_REPOSITORY_ERROR",
                message = "Role repository operation failed",
                details = mapOf("operation" to operation),
                cause = ex,
            )
        }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(JpaRoleRepository::class.java)
    }
}
