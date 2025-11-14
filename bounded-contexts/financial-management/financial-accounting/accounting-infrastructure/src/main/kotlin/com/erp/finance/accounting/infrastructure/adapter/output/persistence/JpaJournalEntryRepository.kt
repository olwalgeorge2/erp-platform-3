package com.erp.finance.accounting.infrastructure.adapter.output.persistence

import com.erp.finance.accounting.application.port.output.JournalEntryRepository
import com.erp.finance.accounting.domain.model.JournalEntry
import com.erp.finance.accounting.infrastructure.persistence.entity.JournalEntryEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaJournalEntryRepository(
    private val entityManager: EntityManager,
) : JournalEntryRepository {
    override fun save(entry: JournalEntry): JournalEntry {
        val existingEntity = entityManager.find(JournalEntryEntity::class.java, entry.id)
        val entity =
            if (existingEntity != null) {
                existingEntity.also { it.updateFrom(entry) }
            } else {
                JournalEntryEntity.from(entry).also {
                    it.version = null
                    entityManager.persist(it)
                }
            }
        entityManager.flush()
        return entity.toDomain()
    }

    override fun findById(
        id: UUID,
        tenantId: UUID,
    ): JournalEntry? =
        entityManager
            .createQuery(
                """
                SELECT DISTINCT e
                FROM JournalEntryEntity e
                LEFT JOIN FETCH e.lines
                WHERE e.id = :id AND e.tenantId = :tenantId
                """.trimIndent(),
                JournalEntryEntity::class.java,
            ).setParameter("id", id)
            .setParameter("tenantId", tenantId)
            .resultList
            .firstOrNull()
            ?.toDomain()
}
