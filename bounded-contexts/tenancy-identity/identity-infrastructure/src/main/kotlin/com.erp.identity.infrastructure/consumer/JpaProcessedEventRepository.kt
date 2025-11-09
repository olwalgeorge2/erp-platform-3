package com.erp.identity.infrastructure.consumer

import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import java.time.Instant

@ApplicationScoped
class JpaProcessedEventRepository(
    private val entityManager: EntityManager,
) : ProcessedEventRepository {
    override fun alreadyProcessed(fingerprint: String): Boolean =
        entityManager
            .createQuery(
                "SELECT COUNT(e) FROM ProcessedEventEntity e WHERE e.fingerprint = :fp",
                Long::class.java,
            ).setParameter("fp", fingerprint)
            .singleResult > 0

    @Transactional
    override fun markProcessed(fingerprint: String) {
        entityManager.persist(
            ProcessedEventEntity(
                fingerprint = fingerprint,
                processedAt = Instant.now(),
            ),
        )
    }
}
