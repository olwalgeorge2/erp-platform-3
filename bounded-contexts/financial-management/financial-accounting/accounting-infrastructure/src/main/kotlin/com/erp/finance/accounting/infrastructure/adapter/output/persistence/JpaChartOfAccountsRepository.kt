package com.erp.finance.accounting.infrastructure.adapter.output.persistence

import com.erp.finance.accounting.application.port.output.ChartOfAccountsRepository
import com.erp.finance.accounting.domain.model.ChartOfAccounts
import com.erp.finance.accounting.domain.model.ChartOfAccountsId
import com.erp.finance.accounting.infrastructure.persistence.entity.ChartOfAccountsEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaChartOfAccountsRepository(
    private val entityManager: EntityManager,
) : ChartOfAccountsRepository {
    override fun save(chartOfAccounts: ChartOfAccounts): ChartOfAccounts {
        val existing =
            entityManager.find(
                ChartOfAccountsEntity::class.java,
                chartOfAccounts.id.value,
            )

        val managedEntity =
            if (existing == null) {
                ChartOfAccountsEntity.from(chartOfAccounts).also {
                    entityManager.persist(it)
                }
            } else {
                existing.apply { updateFrom(chartOfAccounts) }
            }

        entityManager.flush()
        return managedEntity.toDomain()
    }

    override fun findById(
        id: ChartOfAccountsId,
        tenantId: UUID,
    ): ChartOfAccounts? =
        entityManager
            .createQuery(
                """
                SELECT DISTINCT c
                FROM ChartOfAccountsEntity c
                LEFT JOIN FETCH c.accounts
                WHERE c.id = :id AND c.tenantId = :tenantId
                """.trimIndent(),
                ChartOfAccountsEntity::class.java,
            ).setParameter("id", id.value)
            .setParameter("tenantId", tenantId)
            .resultList
            .firstOrNull()
            ?.toDomain()
}
