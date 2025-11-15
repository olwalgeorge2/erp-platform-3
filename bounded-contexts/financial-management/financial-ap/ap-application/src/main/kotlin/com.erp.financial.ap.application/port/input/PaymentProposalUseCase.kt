package com.erp.financial.ap.application.port.input

import com.erp.financial.ap.application.port.input.command.GeneratePaymentProposalCommand
import com.erp.financial.ap.application.port.output.ListPaymentProposalsQuery
import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposal
import java.util.UUID

interface PaymentProposalUseCase {
    fun generateProposal(command: GeneratePaymentProposalCommand): PaymentProposal

    fun listProposals(query: ListPaymentProposalsQuery): List<PaymentProposal>

    fun getProposal(
        tenantId: UUID,
        proposalId: UUID,
    ): PaymentProposal?
}
