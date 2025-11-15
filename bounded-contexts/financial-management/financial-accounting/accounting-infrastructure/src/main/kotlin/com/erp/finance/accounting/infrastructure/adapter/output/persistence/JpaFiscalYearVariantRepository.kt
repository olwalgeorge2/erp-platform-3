package com.erp.finance.accounting.infrastructure.adapter.output.persistence

import com.erp.finance.accounting.application.port.output.FiscalYearVariantRepository
import com.erp.finance.accounting.domain.model.CompanyCodeFiscalYearVariant
import com.erp.finance.accounting.domain.model.FiscalYearVariant
import com.erp.finance.accounting.domain.model.PeriodBlackout
import com.erp.finance.accounting.infrastructure.persistence.entity.CompanyCodeFiscalYearVariantEntity
import com.erp.finance.accounting.infrastructure.persistence.entity.FiscalYearVariantEntity
import com.erp.finance.accounting.infrastructure.persistence.entity.PeriodBlackoutEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaFiscalYearVariantRepository
    @Inject
    constructor(
        private val entityManager: EntityManager,
    ) : FiscalYearVariantRepository {
        override fun save(variant: FiscalYearVariant): FiscalYearVariant {
            val existing = entityManager.find(FiscalYearVariantEntity::class.java, variant.id)
            val entity =
                if (existing == null) {
                    FiscalYearVariantEntity.from(variant).also { entityManager.persist(it) }
                } else {
                    existing.updateFrom(variant)
                    existing
                }
            entityManager.flush()
            return entity.toDomain()
        }

        override fun findById(
            tenantId: UUID,
            id: UUID,
        ): FiscalYearVariant? =
            entityManager
                .find(FiscalYearVariantEntity::class.java, id)
                ?.takeIf { it.tenantId == tenantId }
                ?.toDomain()

        override fun findByTenant(tenantId: UUID): List<FiscalYearVariant> =
            entityManager
                .createQuery(
                    "SELECT v FROM FiscalYearVariantEntity v WHERE v.tenantId = :tenantId ORDER BY v.code",
                    FiscalYearVariantEntity::class.java,
                ).setParameter("tenantId", tenantId)
                .resultList
                .map(FiscalYearVariantEntity::toDomain)

        override fun assignToCompanyCode(assignment: CompanyCodeFiscalYearVariant): CompanyCodeFiscalYearVariant {
            val entity = CompanyCodeFiscalYearVariantEntity.from(assignment)
            val managed = entityManager.merge(entity)
            entityManager.flush()
            return managed.toDomain()
        }

        override fun scheduleBlackout(blackout: PeriodBlackout): PeriodBlackout {
            val existing = entityManager.find(PeriodBlackoutEntity::class.java, blackout.id)
            val entity =
                if (existing == null) {
                    PeriodBlackoutEntity.from(blackout).also { entityManager.persist(it) }
                } else {
                    existing.updateFrom(blackout)
                    existing
                }
            entityManager.flush()
            return entity.toDomain()
        }
    }
