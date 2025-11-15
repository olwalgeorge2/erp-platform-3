package com.erp.finance.accounting.infrastructure.adapter.output.persistence

import com.erp.finance.accounting.application.port.output.CompanyCodeLedgerRepository
import com.erp.finance.accounting.infrastructure.persistence.entity.CompanyCodeLedgerEntity
import com.erp.finance.accounting.infrastructure.persistence.entity.CompanyCodeLedgerKey
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaCompanyCodeLedgerRepository(
    private val entityManager: EntityManager,
) : CompanyCodeLedgerRepository {
    override fun linkLedger(
        companyCodeId: UUID,
        ledgerId: UUID,
    ) {
        val key =
            CompanyCodeLedgerKey(
                companyCodeId = companyCodeId,
                ledgerId = ledgerId,
            )
        val entity = entityManager.find(CompanyCodeLedgerEntity::class.java, key)
        if (entity == null) {
            entityManager.persist(CompanyCodeLedgerEntity(id = key))
        }
    }

    override fun findLedgersForCompanyCode(companyCodeId: UUID): List<UUID> =
        entityManager
            .createQuery(
                """
                SELECT e.id.ledgerId
                FROM CompanyCodeLedgerEntity e
                WHERE e.id.companyCodeId = :companyCodeId
                """.trimIndent(),
                UUID::class.java,
            ).setParameter("companyCodeId", companyCodeId)
            .resultList
}
