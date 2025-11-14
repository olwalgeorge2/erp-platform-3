package com.erp.finance.accounting.infrastructure.outbox

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.time.Instant

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaFinanceOutboxRepository
    @Inject
    constructor(
        private val entityManager: EntityManager,
    ) : FinanceOutboxRepository {
        override fun save(event: FinanceOutboxEventEntity) {
            entityManager.persist(event)
        }

        override fun fetchPending(
            limit: Int,
            maxAttempts: Int,
        ): List<FinanceOutboxEventEntity> =
            entityManager
                .createQuery(
                    """
                    SELECT e
                    FROM FinanceOutboxEventEntity e
                    WHERE e.status IN (:pending, :failed)
                      AND e.failureCount < :maxAttempts
                    ORDER BY e.recordedAt
                    """.trimIndent(),
                    FinanceOutboxEventEntity::class.java,
                ).setParameter("pending", FinanceOutboxEventStatus.PENDING)
                .setParameter("failed", FinanceOutboxEventStatus.FAILED)
                .setParameter("maxAttempts", maxAttempts)
                .setMaxResults(limit)
                .resultList

        override fun markPublished(event: FinanceOutboxEventEntity) {
            event.markPublished()
            entityManager.merge(event)
        }

        override fun markFailed(
            event: FinanceOutboxEventEntity,
            throwable: Throwable,
            maxAttempts: Int,
        ) {
            event.markForRetry(throwable, maxAttempts)
            entityManager.merge(event)
        }

        override fun countPending(maxAttempts: Int): Long =
            entityManager
                .createQuery(
                    """
                    SELECT COUNT(e)
                    FROM FinanceOutboxEventEntity e
                    WHERE e.status IN (:pending, :failed)
                      AND e.failureCount < :maxAttempts
                    """.trimIndent(),
                    java.lang.Long::class.java,
                ).setParameter("pending", FinanceOutboxEventStatus.PENDING)
                .setParameter("failed", FinanceOutboxEventStatus.FAILED)
                .setParameter("maxAttempts", maxAttempts)
                .singleResult
                .toLong()

        override fun deleteOlderThan(
            statuses: Set<FinanceOutboxEventStatus>,
            cutoff: Instant,
        ): Int =
            entityManager
                .createQuery(
                    """
                    DELETE FROM FinanceOutboxEventEntity e
                    WHERE e.status IN :statuses
                      AND e.recordedAt < :cutoff
                    """.trimIndent(),
                ).setParameter("statuses", statuses)
                .setParameter("cutoff", cutoff)
                .executeUpdate()
    }
