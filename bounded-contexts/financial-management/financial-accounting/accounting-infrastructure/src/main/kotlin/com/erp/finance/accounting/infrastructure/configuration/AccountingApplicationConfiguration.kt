package com.erp.finance.accounting.infrastructure.configuration

import com.erp.finance.accounting.application.port.output.AccountingPeriodRepository
import com.erp.finance.accounting.application.port.output.ChartOfAccountsRepository
import com.erp.finance.accounting.application.port.output.FinanceEventPublisher
import com.erp.finance.accounting.application.port.output.JournalEntryRepository
import com.erp.finance.accounting.application.port.output.LedgerRepository
import com.erp.finance.accounting.application.service.AccountingCommandHandler
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces

@ApplicationScoped
class AccountingApplicationConfiguration {
    @Produces
    fun accountingCommandHandler(
        ledgerRepository: LedgerRepository,
        chartRepository: ChartOfAccountsRepository,
        periodRepository: AccountingPeriodRepository,
        journalRepository: JournalEntryRepository,
        eventPublisher: FinanceEventPublisher,
    ): AccountingCommandHandler =
        AccountingCommandHandler(
            ledgerRepository = ledgerRepository,
            chartRepository = chartRepository,
            periodRepository = periodRepository,
            journalRepository = journalRepository,
            eventPublisher = eventPublisher,
        )
}
