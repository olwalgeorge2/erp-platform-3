package com.erp.finance.accounting.application.port.output

import com.erp.finance.accounting.domain.model.JournalEntry
import java.util.UUID

interface JournalEntryRepository {
    fun save(entry: JournalEntry): JournalEntry

    fun findById(
        id: UUID,
        tenantId: UUID,
    ): JournalEntry?
}
