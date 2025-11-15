package com.erp.financial.ar.application.port.output

import com.erp.financial.ar.domain.model.openitem.ArOpenItem
import com.erp.financial.ar.domain.model.openitem.ArOpenItemStatus
import java.time.LocalDate
import java.util.UUID

data class ArOpenItemFilter(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val customerId: UUID? = null,
    val statuses: Set<ArOpenItemStatus> = setOf(ArOpenItemStatus.OPEN, ArOpenItemStatus.PARTIALLY_PAID),
)

interface ArOpenItemRepository {
    fun findByInvoice(invoiceId: UUID): ArOpenItem?

    fun list(filter: ArOpenItemFilter): List<ArOpenItem>

    fun updateReceiptMetadata(
        invoiceId: UUID,
        receiptDate: LocalDate,
    )
}
