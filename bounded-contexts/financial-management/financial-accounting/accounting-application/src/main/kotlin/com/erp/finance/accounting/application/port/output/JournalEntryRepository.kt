package com.erp.finance.accounting.application.port.output

import com.erp.finance.accounting.domain.model.AccountingPeriodId
import com.erp.finance.accounting.domain.model.JournalEntry
import com.erp.finance.accounting.domain.model.LedgerId
import java.util.UUID

interface JournalEntryRepository {
    fun save(entry: JournalEntry): JournalEntry

    fun findById(
        id: UUID,
        tenantId: UUID,
    ): JournalEntry?

    fun findPostedByLedgerAndPeriod(
        ledgerId: LedgerId,
        accountingPeriodId: AccountingPeriodId,
        tenantId: UUID,
    ): List<JournalEntry>
}
