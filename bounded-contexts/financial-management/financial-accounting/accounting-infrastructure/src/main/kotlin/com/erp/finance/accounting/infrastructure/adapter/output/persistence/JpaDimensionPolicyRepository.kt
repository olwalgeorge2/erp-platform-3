package com.erp.finance.accounting.infrastructure.adapter.output.persistence

import com.erp.finance.accounting.application.port.output.DimensionPolicyRepository
import com.erp.finance.accounting.domain.model.AccountDimensionPolicy
import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.DimensionType
import com.erp.finance.accounting.infrastructure.persistence.entity.AccountDimensionPolicyEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaDimensionPolicyRepository
    @Inject
    constructor(
        private val entityManager: EntityManager,
    ) : DimensionPolicyRepository {
        override fun findByTenant(tenantId: UUID): List<AccountDimensionPolicy> =
            entityManager
                .createQuery(
                    "SELECT p FROM AccountDimensionPolicyEntity p WHERE p.tenantId = :tenantId",
                    AccountDimensionPolicyEntity::class.java,
                ).setParameter("tenantId", tenantId)
                .resultList
                .map(AccountDimensionPolicyEntity::toDomain)

        override fun save(policy: AccountDimensionPolicy): AccountDimensionPolicy {
            val existing = entityManager.find(AccountDimensionPolicyEntity::class.java, policy.id)
            val entity =
                if (existing == null) {
                    AccountDimensionPolicyEntity.from(policy).also { entityManager.persist(it) }
                } else {
                    existing.updateFrom(policy)
                    existing
                }
            entityManager.flush()
            return entity.toDomain()
        }

        override fun findByTenantAndAccountType(
            tenantId: UUID,
            accountType: AccountType,
        ): List<AccountDimensionPolicy> =
            entityManager
                .createQuery(
                    """
                    SELECT p FROM AccountDimensionPolicyEntity p
                    WHERE p.tenantId = :tenantId AND p.accountType = :accountType
                    """.trimIndent(),
                    AccountDimensionPolicyEntity::class.java,
                ).setParameter("tenantId", tenantId)
                .setParameter("accountType", accountType)
                .resultList
                .map(AccountDimensionPolicyEntity::toDomain)

        override fun deleteByTenantAndDimension(
            tenantId: UUID,
            dimensionType: DimensionType,
            accountType: AccountType,
        ) {
            entityManager
                .createQuery(
                    """
                    DELETE FROM AccountDimensionPolicyEntity p
                    WHERE p.tenantId = :tenantId AND p.dimensionType = :dimensionType AND p.accountType = :accountType
                    """.trimIndent(),
                ).setParameter("tenantId", tenantId)
                .setParameter("dimensionType", dimensionType)
                .setParameter("accountType", accountType)
                .executeUpdate()
        }
    }
