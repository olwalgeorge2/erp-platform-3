package com.erp.finance.accounting.infrastructure.service

import com.erp.finance.accounting.application.port.input.FinanceCommandUseCase
import com.erp.finance.accounting.application.port.input.command.CloseAccountingPeriodCommand
import com.erp.finance.accounting.application.port.input.command.CreateLedgerCommand
import com.erp.finance.accounting.application.port.input.command.DefineAccountCommand
import com.erp.finance.accounting.application.port.input.command.PostJournalEntryCommand
import com.erp.finance.accounting.application.service.AccountingCommandHandler
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.ChartOfAccounts
import com.erp.finance.accounting.domain.model.JournalEntry
import com.erp.finance.accounting.domain.model.Ledger
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.jboss.logging.Logger

@ApplicationScoped
class FinanceCommandService(
    private val commandHandler: AccountingCommandHandler,
) : FinanceCommandUseCase {
    @Transactional(TxType.REQUIRED)
    override fun createLedger(command: CreateLedgerCommand): Ledger {
        LOGGER.debugf("Creating ledger for tenant=%s chart=%s", command.tenantId, command.chartOfAccountsId)
        return commandHandler.createLedger(command)
    }

    @Transactional(TxType.REQUIRED)
    override fun defineAccount(command: DefineAccountCommand): ChartOfAccounts {
        LOGGER.debugf(
            "Defining account %s for chart=%s tenant=%s",
            command.code,
            command.chartOfAccountsId,
            command.tenantId,
        )
        return commandHandler.defineAccount(command)
    }

    @Transactional(TxType.REQUIRED)
    override fun postJournalEntry(command: PostJournalEntryCommand): JournalEntry {
        LOGGER.debugf(
            "Posting journal entry ledger=%s period=%s tenant=%s lines=%d",
            command.ledgerId,
            command.accountingPeriodId,
            command.tenantId,
            command.lines.size,
        )
        return commandHandler.postJournalEntry(command)
    }

    @Transactional(TxType.REQUIRED)
    override fun closePeriod(command: CloseAccountingPeriodCommand): AccountingPeriod {
        LOGGER.debugf(
            "Closing accounting period ledger=%s period=%s tenant=%s freezeOnly=%s",
            command.ledgerId,
            command.accountingPeriodId,
            command.tenantId,
            command.freezeOnly,
        )
        return commandHandler.closeAccountingPeriod(command)
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(FinanceCommandService::class.java)
    }
}
