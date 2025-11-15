package com.erp.finance.accounting.infrastructure.adapter.output.persistence

import com.erp.finance.accounting.application.port.output.ControlAccountRepository
import com.erp.finance.accounting.domain.model.ControlAccountCategory
import com.erp.finance.accounting.domain.model.ControlAccountConfig
import com.erp.finance.accounting.domain.model.ControlAccountSubLedger
import com.erp.finance.accounting.infrastructure.persistence.entity.ControlAccountConfigEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.time.Instant
import java.util.Locale
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaControlAccountRepository(
    private val entityManager: EntityManager,
) : ControlAccountRepository {
    override fun save(config: ControlAccountConfig): ControlAccountConfig {
        val normalized =
            config.copy(
                dimensionKey = normalizeDimensionKey(config.dimensionKey),
                currency = normalizeCurrency(config.currency),
                updatedAt = Instant.now(),
            )
        val entity =
            findByBusinessKey(
                tenantId = normalized.tenantId,
                companyCodeId = normalized.companyCodeId,
                subLedger = normalized.subLedger,
                category = normalized.category,
                dimensionKey = normalized.dimensionKey,
                currency = normalized.currency,
            )
                ?: ControlAccountConfigEntity.from(normalized).also(entityManager::persist)

        entity.glAccountId = normalized.glAccountId
        entity.updatedAt = normalized.updatedAt
        return entity.toDomain()
    }

    override fun findAccount(
        tenantId: UUID,
        companyCodeId: UUID,
        subLedger: ControlAccountSubLedger,
        category: ControlAccountCategory,
        dimensionKey: String,
        currency: String,
    ): ControlAccountConfig? =
        findByBusinessKey(
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            subLedger = subLedger,
            category = category,
            dimensionKey = normalizeDimensionKey(dimensionKey),
            currency = normalizeCurrency(currency),
        )?.toDomain()

    private fun findByBusinessKey(
        tenantId: UUID,
        companyCodeId: UUID,
        subLedger: ControlAccountSubLedger,
        category: ControlAccountCategory,
        dimensionKey: String,
        currency: String,
    ): ControlAccountConfigEntity? =
        entityManager
            .createQuery(
                """
                SELECT c
                FROM ControlAccountConfigEntity c
                WHERE c.tenantId = :tenantId
                    AND c.companyCodeId = :companyCodeId
                    AND c.subLedger = :subledger
                    AND c.category = :category
                    AND c.dimensionKey = :dimensionKey
                    AND c.currency = :currency
                """.trimIndent(),
                ControlAccountConfigEntity::class.java,
            ).setParameter("tenantId", tenantId)
            .setParameter("companyCodeId", companyCodeId)
            .setParameter("subledger", subLedger)
            .setParameter("category", category)
            .setParameter("dimensionKey", dimensionKey)
            .setParameter("currency", currency)
            .resultList
            .firstOrNull()

    private fun normalizeDimensionKey(dimensionKey: String): String = dimensionKey.uppercase(Locale.ROOT)

    private fun normalizeCurrency(currency: String): String = currency.uppercase(Locale.ROOT)
}
