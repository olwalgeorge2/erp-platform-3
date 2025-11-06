package com.erp.identity.infrastructure.persistence

import com.erp.identity.application.port.output.TenantRepository
import com.erp.identity.domain.model.tenant.Tenant
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.model.tenant.TenantStatus
import com.erp.identity.infrastructure.persistence.entity.TenantEntity
import com.erp.shared.types.results.Result
import com.erp.shared.types.results.Result.Companion.failure
import com.erp.shared.types.results.Result.Companion.success
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceException
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.jboss.logging.Logger
import org.hibernate.exception.ConstraintViolationException

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaTenantRepository(
    private val entityManager: EntityManager,
) : TenantRepository {
    override fun findById(tenantId: TenantId): Result<Tenant?> =
        runQuery("findById") {
            entityManager.find(TenantEntity::class.java, tenantId.value)?.toDomain()
        }

    override fun findBySlug(slug: String): Result<Tenant?> =
        runQuery("findBySlug") {
            entityManager
                .createQuery(
                    """
                    SELECT t FROM TenantEntity t
                    WHERE LOWER(t.slug) = LOWER(:slug)
                    """.trimIndent(),
                    TenantEntity::class.java,
                ).setParameter("slug", slug)
                .resultList
                .firstOrNull()
                ?.toDomain()
        }

    override fun existsBySlug(slug: String): Result<Boolean> =
        runQuery("existsBySlug") {
            entityManager
                .createQuery(
                    """
                    SELECT COUNT(t) FROM TenantEntity t
                    WHERE LOWER(t.slug) = LOWER(:slug)
                    """.trimIndent(),
                    java.lang.Long::class.java,
                ).setParameter("slug", slug)
                .singleResult > 0
        }

    override fun save(tenant: Tenant): Result<Tenant> =
        runCommand("save") {
            val entity = TenantEntity.from(tenant)
            val merged = entityManager.merge(entity)
            entityManager.flush()
            merged.toDomain()
        }

    override fun listTenants(
        status: TenantStatus?,
        limit: Int,
        offset: Int,
    ): Result<List<Tenant>> =
        runQuery("listTenants") {
            val builder = StringBuilder("SELECT t FROM TenantEntity t")
            if (status != null) {
                builder.append(" WHERE t.status = :status")
            }
            builder.append(" ORDER BY t.createdAt DESC")

            val query =
                entityManager
                    .createQuery(builder.toString(), TenantEntity::class.java)
                    .setMaxResults(limit)
                    .setFirstResult(offset)

            if (status != null) {
                query.setParameter("status", status)
            }

            query.resultList.map { it.toDomain() }
        }

    private fun <T> runQuery(
        operation: String,
        block: () -> T,
    ): Result<T> =
        try {
            success(block())
        } catch (ex: PersistenceException) {
            mapPersistenceError(operation, ex)
        }

    private fun <T> runCommand(
        operation: String,
        block: () -> T,
    ): Result<T> = runQuery(operation, block)

    private fun mapPersistenceError(
        operation: String,
        ex: PersistenceException,
    ): Result.Failure {
        val root = ex.rootCause()
        val constraintName =
            (root as? ConstraintViolationException)
                ?.constraintName
                ?.lowercase()
                ?: root.message?.lowercase().orEmpty()

        if (constraintName.contains("uk_identity_tenants_slug")) {
            return failure(
                code = "TENANT_SLUG_EXISTS",
                message = "Tenant slug already exists",
                details = mapOf("operation" to operation),
                cause = ex,
            )
        }

        LOGGER.errorf(ex, "TenantRepository.%s failed", operation)
        return failure(
            code = "TENANT_REPOSITORY_ERROR",
            message = "Tenant repository operation failed",
            details = mapOf("operation" to operation),
            cause = ex,
        )
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(JpaTenantRepository::class.java)
    }

    private fun Throwable.rootCause(): Throwable =
        generateSequence(this) { it.cause }.last()
}
