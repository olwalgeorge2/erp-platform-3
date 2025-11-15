package com.erp.finance.accounting.infrastructure.adapter.output.persistence

import com.erp.finance.accounting.application.port.output.CompanyCodeRepository
import com.erp.finance.accounting.domain.model.CompanyCode
import com.erp.finance.accounting.infrastructure.persistence.entity.CompanyCodeEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaCompanyCodeRepository
    @Inject
    constructor(
        private val entityManager: EntityManager,
    ) : CompanyCodeRepository {
        override fun save(companyCode: CompanyCode): CompanyCode {
            val existing = entityManager.find(CompanyCodeEntity::class.java, companyCode.id)
            val entity =
                if (existing == null) {
                    CompanyCodeEntity.from(companyCode).also { entityManager.persist(it) }
                } else {
                    existing.updateFrom(companyCode)
                    existing
                }
            entityManager.flush()
            return entity.toDomain()
        }

        override fun findById(
            tenantId: UUID,
            id: UUID,
        ): CompanyCode? =
            entityManager
                .find(CompanyCodeEntity::class.java, id)
                ?.takeIf { it.tenantId == tenantId }
                ?.toDomain()

        override fun findByTenant(tenantId: UUID): List<CompanyCode> =
            entityManager
                .createQuery(
                    "SELECT c FROM CompanyCodeEntity c WHERE c.tenantId = :tenantId ORDER BY c.code",
                    CompanyCodeEntity::class.java,
                ).setParameter("tenantId", tenantId)
                .resultList
                .map(CompanyCodeEntity::toDomain)
    }
