package com.erp.identity.infrastructure.persistence

import com.erp.identity.application.port.output.TenantRepository
import com.erp.identity.domain.model.tenant.Tenant
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.model.tenant.TenantStatus
import com.erp.identity.infrastructure.persistence.entity.TenantEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaTenantRepository(
    private val entityManager: EntityManager,
) : TenantRepository {
    override fun findById(tenantId: TenantId): Tenant? =
        entityManager
            .find(TenantEntity::class.java, tenantId.value)
            ?.toDomain()

    override fun findBySlug(slug: String): Tenant? =
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

    override fun existsBySlug(slug: String): Boolean =
        entityManager
            .createQuery(
                """
                SELECT COUNT(t) FROM TenantEntity t 
                WHERE LOWER(t.slug) = LOWER(:slug)
                """.trimIndent(),
                java.lang.Long::class.java,
            ).setParameter("slug", slug)
            .singleResult > 0

    override fun save(tenant: Tenant): Tenant {
        val entity = TenantEntity.from(tenant)
        val merged = entityManager.merge(entity)
        entityManager.flush()
        return merged.toDomain()
    }

    override fun listTenants(
        status: TenantStatus?,
        limit: Int,
        offset: Int,
    ): List<Tenant> {
        val builder =
            StringBuilder("SELECT t FROM TenantEntity t")
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

        return query.resultList.map { it.toDomain() }
    }
}
