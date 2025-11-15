package com.erp.financial.ar.application.port.output

import com.erp.financial.ar.domain.model.invoice.CustomerInvoice
import java.util.UUID

interface ReceivablePostingPort {
    fun postCustomerInvoice(invoice: CustomerInvoice): ReceivablePostingResult
}

data class ReceivablePostingResult(
    val journalEntryId: UUID,
    val ledgerId: UUID,
    val accountingPeriodId: UUID,
)
