package com.erp.finance.accounting.application.port.output

import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.AccountingPeriodStatus
import com.erp.finance.accounting.domain.model.JournalEntry

interface FinanceEventPublisher {
    fun publishJournalPosted(entry: JournalEntry)

    fun publishPeriodUpdated(
        period: AccountingPeriod,
        previousStatus: AccountingPeriodStatus,
    )
}
