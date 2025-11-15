package com.erp.financial.ap.infrastructure.adapter.output.persistence

import com.erp.financial.ap.application.port.output.BillRepository
import com.erp.financial.ap.domain.model.bill.BillId
import com.erp.financial.ap.domain.model.bill.BillStatus
import com.erp.financial.ap.domain.model.bill.VendorBill
import com.erp.financial.ap.domain.model.openitem.ApOpenItemStatus
import com.erp.financial.ap.infrastructure.persistence.entity.ApOpenItemEntity
import com.erp.financial.ap.infrastructure.persistence.entity.VendorBillEntity
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
class JpaBillRepository
    @Inject
    constructor(
        private val entityManager: EntityManager,
    ) : BillRepository {
        override fun save(bill: VendorBill): VendorBill {
            val managed = entityManager.find(VendorBillEntity::class.java, bill.id.value)
            val entity =
                if (managed == null) {
                    VendorBillEntity.from(bill).also {
                        entityManager.persist(it)
                        syncOpenItem(it)
                    }
                } else {
                    managed.updateFrom(bill)
                    syncOpenItem(managed)
                    managed
                }
            entityManager.flush()
            return entity.toDomain()
        }

        override fun findById(
            tenantId: UUID,
            billId: BillId,
        ): VendorBill? =
            entityManager
                .find(VendorBillEntity::class.java, billId.value)
                ?.takeIf { it.tenantId == tenantId }
                ?.toDomain()

        override fun list(
            tenantId: UUID,
            companyCodeId: UUID?,
            vendorId: UUID?,
            status: BillStatus?,
            dueBefore: LocalDate?,
        ): List<VendorBill> {
            val builder = StringBuilder("SELECT b FROM VendorBillEntity b WHERE b.tenantId = :tenantId")
            if (companyCodeId != null) {
                builder.append(" AND b.companyCodeId = :companyCodeId")
            }
            if (vendorId != null) {
                builder.append(" AND b.vendorId = :vendorId")
            }
            if (status != null) {
                builder.append(" AND b.status = :status")
            }
            if (dueBefore != null) {
                builder.append(" AND b.dueDate <= :dueBefore")
            }
            builder.append(" ORDER BY b.dueDate ASC")

            val query =
                entityManager
                    .createQuery(builder.toString(), VendorBillEntity::class.java)
                    .setParameter("tenantId", tenantId)
            companyCodeId?.let { query.setParameter("companyCodeId", it) }
            vendorId?.let { query.setParameter("vendorId", it) }
            status?.let { query.setParameter("status", it) }
            dueBefore?.let { query.setParameter("dueBefore", it) }
            return query.resultList.map(VendorBillEntity::toDomain)
        }

        private fun syncOpenItem(entity: VendorBillEntity) {
            val shouldTrack =
                when (entity.status) {
                    BillStatus.POSTED, BillStatus.PARTIALLY_PAID, BillStatus.PAID -> true
                    else -> false
                }
            val existing = findOpenItem(entity.id)
            if (!shouldTrack) {
                existing?.let(entityManager::remove)
                return
            }
            val tracked =
                existing
                    ?: ApOpenItemEntity(
                        invoice = entity,
                        tenantId = entity.tenantId,
                        companyCodeId = entity.companyCodeId,
                        vendorId = entity.vendorId,
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
            val clearedAmount = entity.paidAmount
            val outstanding = (originalAmount - clearedAmount).coerceAtLeast(0)
            tracked.invoice = entity
            tracked.tenantId = entity.tenantId
            tracked.companyCodeId = entity.companyCodeId
            tracked.vendorId = entity.vendorId
            tracked.invoiceNumber = entity.invoiceNumber
            tracked.invoiceDate = entity.invoiceDate
            tracked.currency = entity.currency
            tracked.originalAmountMinor = originalAmount
            tracked.clearedAmountMinor = clearedAmount
            tracked.amountOutstanding = outstanding
            tracked.dueDate = entity.dueDate
            tracked.status = mapStatus(entity.status, outstanding)
            tracked.journalEntryId = entity.journalEntryId
            tracked.paymentTermsCode = entity.paymentTermsCode
            tracked.paymentTermsType = entity.paymentTermsType
            tracked.paymentTermsDueDays = entity.paymentTermsDueDays
            tracked.paymentTermsDiscountPercentage = entity.paymentTermsDiscountPercentage
            tracked.paymentTermsDiscountDays = entity.paymentTermsDiscountDays
            tracked.cashDiscountDueDate =
                entity.paymentTermsDiscountDays?.let { entity.invoiceDate.plusDays(it.toLong()) }
            tracked.updatedAt = Instant.now()
        }

        private fun mapStatus(
            billStatus: BillStatus,
            outstanding: Long,
        ): ApOpenItemStatus =
            when {
                outstanding <= 0 -> ApOpenItemStatus.CLEARED
                billStatus == BillStatus.PARTIALLY_PAID -> ApOpenItemStatus.PARTIALLY_PAID
                else -> ApOpenItemStatus.OPEN
            }

        private fun findOpenItem(invoiceId: UUID): ApOpenItemEntity? =
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
    }
