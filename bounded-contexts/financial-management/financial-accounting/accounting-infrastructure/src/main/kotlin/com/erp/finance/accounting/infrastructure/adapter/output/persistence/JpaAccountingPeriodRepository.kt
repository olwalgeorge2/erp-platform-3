package com.erp.finance.accounting.infrastructure.adapter.output.persistence

import com.erp.finance.accounting.application.port.output.AccountingPeriodRepository
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.AccountingPeriodId
import com.erp.finance.accounting.domain.model.AccountingPeriodStatus
import com.erp.finance.accounting.domain.model.LedgerId
import com.erp.finance.accounting.infrastructure.persistence.entity.AccountingPeriodEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaAccountingPeriodRepository(
    private val entityManager: EntityManager,
) : AccountingPeriodRepository {
    override fun save(period: AccountingPeriod): AccountingPeriod {
        val existingEntity = entityManager.find(AccountingPeriodEntity::class.java, period.id.value)
        val entity =
            if (existingEntity != null) {
                existingEntity.apply {
                    ledgerId = period.ledgerId.value
                    tenantId = period.tenantId
                    periodCode = period.code
                    startDate = period.startDate
                    endDate = period.endDate
                    status = period.status
                    updatedAt = java.time.Instant.now()
                }
            } else {
                AccountingPeriodEntity.from(period).also {
                    it.version = null
                    entityManager.persist(it)
                }
            }
        entityManager.flush()
        return entity.toDomain()
    }

    override fun findById(
        id: AccountingPeriodId,
        tenantId: UUID,
    ): AccountingPeriod? =
        entityManager
            .createQuery(
                """
                SELECT p
                FROM AccountingPeriodEntity p
                WHERE p.id = :id AND p.tenantId = :tenantId
                """.trimIndent(),
                AccountingPeriodEntity::class.java,
            ).setParameter("id", id.value)
            .setParameter("tenantId", tenantId)
            .resultList
            .firstOrNull()
            ?.toDomain()

    override fun findOpenByLedger(
        ledgerId: LedgerId,
        tenantId: UUID,
    ): List<AccountingPeriod> =
        entityManager
            .createQuery(
                """
                SELECT p
                FROM AccountingPeriodEntity p
                WHERE p.ledgerId = :ledgerId
                  AND p.tenantId = :tenantId
                  AND p.status IN :statuses
                ORDER BY p.startDate
                """.trimIndent(),
                AccountingPeriodEntity::class.java,
            ).setParameter("ledgerId", ledgerId.value)
            .setParameter("tenantId", tenantId)
            .setParameter("statuses", setOf(AccountingPeriodStatus.OPEN, AccountingPeriodStatus.FROZEN))
            .resultList
            .map(AccountingPeriodEntity::toDomain)
}
