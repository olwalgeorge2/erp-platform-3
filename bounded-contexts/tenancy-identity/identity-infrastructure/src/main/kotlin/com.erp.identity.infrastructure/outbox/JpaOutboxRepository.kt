package com.erp.identity.infrastructure.outbox

import com.erp.shared.types.results.Result
import com.erp.shared.types.results.Result.Companion.failure
import com.erp.shared.types.results.Result.Companion.success
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceException
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.jboss.logging.Logger

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaOutboxRepository(
    private val entityManager: EntityManager,
) : OutboxRepository {
    override fun save(event: OutboxEventEntity): Result<Unit> =
        runCatching {
            entityManager.persist(event)
            success(Unit)
        }.getOrElse { throwable ->
            LOGGER.errorf(throwable, "Failed to persist outbox event %s", event.eventId)
            failure(
                code = "OUTBOX_PERSISTENCE_ERROR",
                message = "Unable to persist outbox event",
                details = mapOf("eventId" to event.eventId.toString()),
                cause = throwable,
            )
        }

    override fun fetchPending(
        limit: Int,
        maxAttemptsBeforeFailure: Int,
    ): List<OutboxEventEntity> =
        entityManager
            .createQuery(
                """
                SELECT e FROM OutboxEventEntity e
                WHERE e.status = :pending
                   OR (e.status = :failed AND e.failureCount < :maxAttempts)
                ORDER BY e.recordedAt ASC
                """.trimIndent(),
                OutboxEventEntity::class.java,
            ).setParameter("pending", OutboxEventStatus.PENDING)
            .setParameter("failed", OutboxEventStatus.FAILED)
            .setParameter("maxAttempts", maxAttemptsBeforeFailure)
            .setMaxResults(limit)
            .resultList

    override fun markPublished(event: OutboxEventEntity): Result<Unit> =
        update(event) {
            it.markPublished()
        }

    override fun markFailed(
        event: OutboxEventEntity,
        failure: Throwable,
        maxAttemptsBeforeFailure: Int,
    ): Result<Unit> =
        update(event) {
            it.markForRetry(failure, maxAttemptsBeforeFailure)
        }

    private fun update(
        event: OutboxEventEntity,
        block: (OutboxEventEntity) -> Unit,
    ): Result<Unit> =
        try {
            val managed = entityManager.find(OutboxEventEntity::class.java, event.id)
                ?: return failure(
                    code = "OUTBOX_EVENT_NOT_FOUND",
                    message = "Outbox event no longer exists",
                    details = mapOf("eventId" to event.eventId.toString()),
                )

            block(managed)
            entityManager.merge(managed)
            success(Unit)
        } catch (ex: PersistenceException) {
            LOGGER.errorf(ex, "Failed to update outbox event %s", event.eventId)
            failure(
                code = "OUTBOX_PERSISTENCE_ERROR",
                message = "Unable to update outbox event",
                details = mapOf("eventId" to event.eventId.toString()),
                cause = ex,
            )
        }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(JpaOutboxRepository::class.java)
    }
}
