package com.erp.financial.ar.infrastructure.adapter.output.persistence

import com.erp.financial.ar.application.port.output.ArOpenItemFilter
import com.erp.financial.ar.application.port.output.ArOpenItemRepository
import com.erp.financial.ar.domain.model.openitem.ArOpenItem
import com.erp.financial.ar.infrastructure.persistence.entity.ArOpenItemEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaArOpenItemRepository(
    private val entityManager: EntityManager,
) : ArOpenItemRepository {
    override fun findByInvoice(invoiceId: UUID): ArOpenItem? =
        entityManager
            .createQuery(
                """
                SELECT oi
                FROM ArOpenItemEntity oi
                WHERE oi.invoice.id = :invoiceId
                """.trimIndent(),
                ArOpenItemEntity::class.java,
            ).setParameter("invoiceId", invoiceId)
            .resultList
            .firstOrNull()
            ?.toDomain()

    override fun list(filter: ArOpenItemFilter): List<ArOpenItem> {
        val builder = StringBuilder("SELECT oi FROM ArOpenItemEntity oi WHERE oi.tenantId = :tenantId")
        filter.companyCodeId?.let { builder.append(" AND oi.companyCodeId = :companyCodeId") }
        filter.customerId?.let { builder.append(" AND oi.customerId = :customerId") }
        if (filter.statuses.isNotEmpty()) {
            builder.append(" AND oi.status IN :statuses")
        }
        builder.append(" ORDER BY oi.dueDate ASC")

        val query =
            entityManager
                .createQuery(builder.toString(), ArOpenItemEntity::class.java)
                .setParameter("tenantId", filter.tenantId)
        filter.companyCodeId?.let { query.setParameter("companyCodeId", it) }
        filter.customerId?.let { query.setParameter("customerId", it) }
        if (filter.statuses.isNotEmpty()) {
            query.setParameter("statuses", filter.statuses)
        }
        return query.resultList.map(ArOpenItemEntity::toDomain)
    }

    override fun updateReceiptMetadata(
        invoiceId: UUID,
        receiptDate: LocalDate,
    ) {
        entityManager
            .createQuery(
                """
                UPDATE ArOpenItemEntity oi
                SET oi.lastReceiptDate = :receiptDate,
                    oi.updatedAt = :updatedAt
                WHERE oi.invoice.id = :invoiceId
                """.trimIndent(),
            ).setParameter("receiptDate", receiptDate)
            .setParameter("updatedAt", Instant.now())
            .setParameter("invoiceId", invoiceId)
            .executeUpdate()
    }
}
