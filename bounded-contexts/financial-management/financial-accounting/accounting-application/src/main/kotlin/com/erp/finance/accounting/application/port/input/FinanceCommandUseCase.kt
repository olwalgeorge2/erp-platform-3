package com.erp.finance.accounting.application.port.input

import com.erp.finance.accounting.application.port.input.command.CloseAccountingPeriodCommand
import com.erp.finance.accounting.application.port.input.command.CreateLedgerCommand
import com.erp.finance.accounting.application.port.input.command.DefineAccountCommand
import com.erp.finance.accounting.application.port.input.command.PostJournalEntryCommand
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.ChartOfAccounts
import com.erp.finance.accounting.domain.model.JournalEntry
import com.erp.finance.accounting.domain.model.Ledger

interface FinanceCommandUseCase {
    fun createLedger(command: CreateLedgerCommand): Ledger

    fun defineAccount(command: DefineAccountCommand): ChartOfAccounts

    fun postJournalEntry(command: PostJournalEntryCommand): JournalEntry

    fun closePeriod(command: CloseAccountingPeriodCommand): AccountingPeriod
}
