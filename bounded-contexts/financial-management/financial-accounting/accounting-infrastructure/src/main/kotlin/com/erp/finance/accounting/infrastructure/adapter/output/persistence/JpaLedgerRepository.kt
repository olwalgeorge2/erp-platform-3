package com.erp.finance.accounting.infrastructure.adapter.output.persistence

import com.erp.finance.accounting.application.port.output.LedgerRepository
import com.erp.finance.accounting.domain.model.Ledger
import com.erp.finance.accounting.domain.model.LedgerId
import com.erp.finance.accounting.infrastructure.persistence.entity.LedgerEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaLedgerRepository(
    private val entityManager: EntityManager,
) : LedgerRepository {
    override fun save(ledger: Ledger): Ledger {
        val existingEntity = entityManager.find(LedgerEntity::class.java, ledger.id.value)
        val entity =
            if (existingEntity != null) {
                existingEntity.apply {
                    chartOfAccountsId = ledger.chartOfAccountsId.value
                    baseCurrency = ledger.baseCurrency
                    status = ledger.status.name
                    updatedAt = ledger.updatedAt
                }
            } else {
                LedgerEntity.from(ledger).also {
                    it.version = null
                    entityManager.persist(it)
                }
            }
        entityManager.flush()
        return entity.toDomain()
    }

    override fun findById(
        id: LedgerId,
        tenantId: UUID,
    ): Ledger? =
        entityManager
            .createQuery(
                """
                SELECT l
                FROM LedgerEntity l
                WHERE l.id = :id AND l.tenantId = :tenantId
                """.trimIndent(),
                LedgerEntity::class.java,
            ).setParameter("id", id.value)
            .setParameter("tenantId", tenantId)
            .resultList
            .firstOrNull()
            ?.toDomain()
}
