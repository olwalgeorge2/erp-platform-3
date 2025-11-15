package com.erp.financial.ap.application.port.output

import com.erp.financial.ap.domain.model.bill.VendorBill
import java.util.UUID

interface FinancialPostingPort {
    fun postVendorBill(bill: VendorBill): JournalPostingResult
}

data class JournalPostingResult(
    val journalEntryId: UUID,
    val ledgerId: UUID,
    val accountingPeriodId: UUID,
)
