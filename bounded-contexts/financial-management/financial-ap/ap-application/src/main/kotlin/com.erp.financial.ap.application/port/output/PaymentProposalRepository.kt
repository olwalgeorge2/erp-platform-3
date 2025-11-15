package com.erp.financial.ap.application.port.output

import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposal
import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposalStatus
import java.util.UUID

data class ListPaymentProposalsQuery(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val status: PaymentProposalStatus? = null,
)

interface PaymentProposalRepository {
    fun save(proposal: PaymentProposal): PaymentProposal

    fun findById(
        tenantId: UUID,
        proposalId: UUID,
    ): PaymentProposal?

    fun list(query: ListPaymentProposalsQuery): List<PaymentProposal>
}
