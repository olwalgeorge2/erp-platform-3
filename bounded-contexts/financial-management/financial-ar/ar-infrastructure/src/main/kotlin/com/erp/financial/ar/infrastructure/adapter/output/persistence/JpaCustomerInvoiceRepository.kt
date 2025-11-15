package com.erp.financial.ar.infrastructure.adapter.output.persistence

import com.erp.financial.ar.application.port.output.CustomerInvoiceRepository
import com.erp.financial.ar.domain.model.invoice.CustomerInvoice
import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceId
import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceStatus
import com.erp.financial.ar.domain.model.openitem.ArOpenItemStatus
import com.erp.financial.ar.infrastructure.persistence.entity.ArOpenItemEntity
import com.erp.financial.ar.infrastructure.persistence.entity.CustomerInvoiceEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaCustomerInvoiceRepository
    @Inject
    constructor(
        private val entityManager: EntityManager,
    ) : CustomerInvoiceRepository {
        override fun save(invoice: CustomerInvoice): CustomerInvoice {
            val managed = entityManager.find(CustomerInvoiceEntity::class.java, invoice.id.value)
            val entity =
                if (managed == null) {
                    CustomerInvoiceEntity.from(invoice).also {
                        entityManager.persist(it)
                        syncOpenItem(it)
                    }
                } else {
                    managed.updateFrom(invoice)
                    syncOpenItem(managed)
                    managed
                }
            entityManager.flush()
            return entity.toDomain()
        }

        override fun findById(
            tenantId: UUID,
            invoiceId: CustomerInvoiceId,
        ): CustomerInvoice? =
            entityManager
                .find(CustomerInvoiceEntity::class.java, invoiceId.value)
                ?.takeIf { it.tenantId == tenantId }
                ?.toDomain()

        override fun list(
            tenantId: UUID,
            companyCodeId: UUID?,
            customerId: UUID?,
            status: CustomerInvoiceStatus?,
            dueBefore: LocalDate?,
        ): List<CustomerInvoice> {
            val builder = StringBuilder("SELECT i FROM CustomerInvoiceEntity i WHERE i.tenantId = :tenantId")
            companyCodeId?.let { builder.append(" AND i.companyCodeId = :companyCodeId") }
            customerId?.let { builder.append(" AND i.customerId = :customerId") }
            status?.let { builder.append(" AND i.status = :status") }
            dueBefore?.let { builder.append(" AND i.dueDate <= :dueBefore") }
            builder.append(" ORDER BY i.dueDate ASC")

            val query =
                entityManager
                    .createQuery(builder.toString(), CustomerInvoiceEntity::class.java)
                    .setParameter("tenantId", tenantId)
            companyCodeId?.let { query.setParameter("companyCodeId", it) }
            customerId?.let { query.setParameter("customerId", it) }
            status?.let { query.setParameter("status", it) }
            dueBefore?.let { query.setParameter("dueBefore", it) }
            return query.resultList.map(CustomerInvoiceEntity::toDomain)
        }

        private fun syncOpenItem(entity: CustomerInvoiceEntity) {
            val shouldTrack =
                when (entity.status) {
                    CustomerInvoiceStatus.POSTED,
                    CustomerInvoiceStatus.PARTIALLY_PAID,
                    CustomerInvoiceStatus.PAID,
                    -> true
                    else -> false
                }
            val existing = findOpenItem(entity.id)
            if (!shouldTrack) {
                existing?.let(entityManager::remove)
                return
            }
            val tracked =
                existing
                    ?: ArOpenItemEntity(
                        invoice = entity,
                        tenantId = entity.tenantId,
                        companyCodeId = entity.companyCodeId,
                        customerId = entity.customerId,
                        invoiceNumber = entity.invoiceNumber,
                        invoiceDate = entity.invoiceDate,
                        currency = entity.currency,
                        originalAmountMinor = 0,
                        clearedAmountMinor = 0,
                        amountOutstanding = 0,
                        dueDate = entity.dueDate,
                        paymentTermsCode = entity.paymentTermsCode,
                        paymentTermsType = entity.paymentTermsType,
                        paymentTermsDueDays = entity.paymentTermsDueDays,
                    ).also(entityManager::persist)
            val originalAmount = entity.netAmount + entity.taxAmount
            val clearedAmount = entity.receivedAmount
            val outstanding = (originalAmount - clearedAmount).coerceAtLeast(0)
            tracked.invoice = entity
            tracked.tenantId = entity.tenantId
            tracked.companyCodeId = entity.companyCodeId
            tracked.customerId = entity.customerId
            tracked.invoiceNumber = entity.invoiceNumber
            tracked.invoiceDate = entity.invoiceDate
            tracked.currency = entity.currency
            tracked.originalAmountMinor = originalAmount
            tracked.clearedAmountMinor = clearedAmount
            tracked.amountOutstanding = outstanding
            tracked.dueDate = entity.dueDate
            tracked.status = mapStatus(entity.status, outstanding)
            tracked.paymentTermsCode = entity.paymentTermsCode
            tracked.paymentTermsType = entity.paymentTermsType
            tracked.paymentTermsDueDays = entity.paymentTermsDueDays
            tracked.paymentTermsDiscountPercentage = entity.paymentTermsDiscountPercentage
            tracked.paymentTermsDiscountDays = entity.paymentTermsDiscountDays
            tracked.cashDiscountDueDate =
                entity.paymentTermsDiscountDays?.let { entity.invoiceDate.plusDays(it.toLong()) }
            tracked.journalEntryId = entity.journalEntryId
            tracked.updatedAt = Instant.now()
        }

        private fun mapStatus(
            status: CustomerInvoiceStatus,
            outstanding: Long,
        ): ArOpenItemStatus =
            when {
                outstanding <= 0 -> ArOpenItemStatus.CLEARED
                status == CustomerInvoiceStatus.PARTIALLY_PAID -> ArOpenItemStatus.PARTIALLY_PAID
                else -> ArOpenItemStatus.OPEN
            }

        private fun findOpenItem(invoiceId: UUID): ArOpenItemEntity? =
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
    }
