package com.erp.financial.ap.application.port.output

import com.erp.financial.ap.domain.model.openitem.ApOpenItem
import com.erp.financial.ap.domain.model.openitem.ApOpenItemStatus
import java.time.LocalDate
import java.util.UUID

data class ApOpenItemFilter(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val vendorId: UUID? = null,
    val statuses: Set<ApOpenItemStatus> = setOf(ApOpenItemStatus.OPEN, ApOpenItemStatus.PARTIALLY_PAID),
    val dueBefore: LocalDate? = null,
)

interface ApOpenItemRepository {
    fun findByInvoice(invoiceId: UUID): ApOpenItem?

    fun findById(
        tenantId: UUID,
        openItemId: UUID,
    ): ApOpenItem?

    fun list(filter: ApOpenItemFilter): List<ApOpenItem>

    fun assignToProposal(
        tenantId: UUID,
        openItemIds: Collection<UUID>,
        proposalId: UUID,
    )

    fun clearProposalLock(proposalId: UUID)

    fun updatePaymentMetadata(
        invoiceId: UUID,
        paymentDate: LocalDate,
    )
}
