package com.erp.financial.ap.infrastructure.adapter.output.persistence

import com.erp.financial.ap.application.port.output.ApOpenItemFilter
import com.erp.financial.ap.application.port.output.ApOpenItemRepository
import com.erp.financial.ap.domain.model.openitem.ApOpenItem
import com.erp.financial.ap.infrastructure.persistence.entity.ApOpenItemEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaApOpenItemRepository(
    private val entityManager: EntityManager,
) : ApOpenItemRepository {
    override fun findByInvoice(invoiceId: UUID): ApOpenItem? =
        entityManager
            .createQuery(
                """
                SELECT oi
                FROM ApOpenItemEntity oi
                WHERE oi.invoice.id = :invoiceId
                """.trimIndent(),
                ApOpenItemEntity::class.java,
            ).setParameter("invoiceId", invoiceId)
            .resultList
            .firstOrNull()
            ?.toDomain()

    override fun findById(
        tenantId: UUID,
        openItemId: UUID,
    ): ApOpenItem? =
        entityManager.find(ApOpenItemEntity::class.java, openItemId)
            ?.takeIf { it.tenantId == tenantId }
            ?.toDomain()

    override fun list(filter: ApOpenItemFilter): List<ApOpenItem> {
        val builder = StringBuilder("SELECT oi FROM ApOpenItemEntity oi WHERE oi.tenantId = :tenantId")
        filter.companyCodeId?.let { builder.append(" AND oi.companyCodeId = :companyCodeId") }
        filter.vendorId?.let { builder.append(" AND oi.vendorId = :vendorId") }
        if (filter.statuses.isNotEmpty()) {
            builder.append(" AND oi.status IN :statuses")
        }
        filter.dueBefore?.let { builder.append(" AND oi.dueDate <= :dueBefore") }
        builder.append(" ORDER BY oi.dueDate ASC")

        val query =
            entityManager
                .createQuery(builder.toString(), ApOpenItemEntity::class.java)
                .setParameter("tenantId", filter.tenantId)
        filter.companyCodeId?.let { query.setParameter("companyCodeId", it) }
        filter.vendorId?.let { query.setParameter("vendorId", it) }
        if (filter.statuses.isNotEmpty()) {
            query.setParameter("statuses", filter.statuses)
        }
        filter.dueBefore?.let { query.setParameter("dueBefore", it) }
        return query.resultList.map(ApOpenItemEntity::toDomain)
    }

    override fun assignToProposal(
        tenantId: UUID,
        openItemIds: Collection<UUID>,
        proposalId: UUID,
    ) {
        if (openItemIds.isEmpty()) {
            return
        }
        entityManager
            .createQuery(
                """
                UPDATE ApOpenItemEntity oi
                SET oi.proposalId = :proposalId,
                    oi.updatedAt = :updatedAt
                WHERE oi.tenantId = :tenantId
                    AND oi.id IN :ids
                    AND oi.proposalId IS NULL
                """.trimIndent(),
            ).setParameter("proposalId", proposalId)
            .setParameter("tenantId", tenantId)
            .setParameter("ids", openItemIds)
            .setParameter("updatedAt", Instant.now())
            .executeUpdate()
    }

    override fun clearProposalLock(proposalId: UUID) {
        entityManager
            .createQuery(
                """
                UPDATE ApOpenItemEntity oi
                SET oi.proposalId = NULL,
                    oi.updatedAt = :updatedAt
                WHERE oi.proposalId = :proposalId
                """.trimIndent(),
            ).setParameter("proposalId", proposalId)
            .setParameter("updatedAt", Instant.now())
            .executeUpdate()
    }

    override fun updatePaymentMetadata(
        invoiceId: UUID,
        paymentDate: LocalDate,
    ) {
        entityManager
            .createQuery(
                """
                UPDATE ApOpenItemEntity oi
                SET oi.lastPaymentDate = :paymentDate,
                    oi.updatedAt = :updatedAt
                WHERE oi.invoice.id = :invoiceId
                """.trimIndent(),
            ).setParameter("paymentDate", paymentDate)
            .setParameter("updatedAt", Instant.now())
            .setParameter("invoiceId", invoiceId)
            .executeUpdate()
    }
}
