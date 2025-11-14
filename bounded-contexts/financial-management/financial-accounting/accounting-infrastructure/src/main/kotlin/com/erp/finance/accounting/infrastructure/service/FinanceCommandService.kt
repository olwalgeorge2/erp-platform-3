package com.erp.finance.accounting.infrastructure.service

import com.erp.finance.accounting.application.port.input.FinanceCommandUseCase
import com.erp.finance.accounting.application.port.input.command.CloseAccountingPeriodCommand
import com.erp.finance.accounting.application.port.input.command.CreateLedgerCommand
import com.erp.finance.accounting.application.port.input.command.DefineAccountCommand
import com.erp.finance.accounting.application.port.input.command.PostJournalEntryCommand
import com.erp.finance.accounting.application.port.input.command.RunCurrencyRevaluationCommand
import com.erp.finance.accounting.application.service.AccountingCommandHandler
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.ChartOfAccounts
import com.erp.finance.accounting.domain.model.JournalEntry
import com.erp.finance.accounting.domain.model.Ledger
import io.micrometer.core.annotation.Counted
import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.jboss.logging.Logger

@ApplicationScoped
class FinanceCommandService(
    private val commandHandler: AccountingCommandHandler,
    meterRegistry: MeterRegistry,
) : FinanceCommandUseCase {
    private val journalPostedCounter = meterRegistry.counter("finance.command.journal.posted.total")
    private val periodCloseCounter = meterRegistry.counter("finance.command.period.close.total")
    private val ledgerCreatedCounter = meterRegistry.counter("finance.command.ledger.created.total")
    private val accountDefinedCounter = meterRegistry.counter("finance.command.account.defined.total")
    private val journalLinesSummary: DistributionSummary =
        DistributionSummary
            .builder("finance.command.journal.lines")
            .description("Number of lines posted per journal entry")
            .publishPercentiles(0.5, 0.9, 0.99)
            .register(meterRegistry)
    private val revaluationCounter = meterRegistry.counter("finance.command.fx_revaluation.total")

    @Transactional(TxType.REQUIRED)
    @Timed(value = "finance.command.create_ledger.duration", extraTags = ["command", "createLedger"])
    @Counted(value = "finance.command.create_ledger.calls", extraTags = ["command", "createLedger"])
    override fun createLedger(command: CreateLedgerCommand): Ledger {
        LOGGER.debugf("Creating ledger for tenant=%s chart=%s", command.tenantId, command.chartOfAccountsId)
        return commandHandler
            .createLedger(command)
            .also { ledgerCreatedCounter.increment() }
    }

    @Transactional(TxType.REQUIRED)
    @Timed(value = "finance.command.define_account.duration", extraTags = ["command", "defineAccount"])
    @Counted(value = "finance.command.define_account.calls", extraTags = ["command", "defineAccount"])
    override fun defineAccount(command: DefineAccountCommand): ChartOfAccounts {
        LOGGER.debugf(
            "Defining account %s for chart=%s tenant=%s",
            command.code,
            command.chartOfAccountsId,
            command.tenantId,
        )
        return commandHandler
            .defineAccount(command)
            .also { accountDefinedCounter.increment() }
    }

    @Transactional(TxType.REQUIRED)
    @Timed(value = "finance.command.post_journal.duration", extraTags = ["command", "postJournalEntry"])
    @Counted(value = "finance.command.post_journal.calls", extraTags = ["command", "postJournalEntry"])
    override fun postJournalEntry(command: PostJournalEntryCommand): JournalEntry {
        LOGGER.debugf(
            "Posting journal entry ledger=%s period=%s tenant=%s lines=%d",
            command.ledgerId,
            command.accountingPeriodId,
            command.tenantId,
            command.lines.size,
        )
        val result = commandHandler.postJournalEntry(command)
        journalPostedCounter.increment()
        journalLinesSummary.record(command.lines.size.toDouble())
        return result
    }

    @Transactional(TxType.REQUIRED)
    @Timed(value = "finance.command.close_period.duration", extraTags = ["command", "closePeriod"])
    @Counted(value = "finance.command.close_period.calls", extraTags = ["command", "closePeriod"])
    override fun closePeriod(command: CloseAccountingPeriodCommand): AccountingPeriod {
        LOGGER.debugf(
            "Closing accounting period ledger=%s period=%s tenant=%s freezeOnly=%s",
            command.ledgerId,
            command.accountingPeriodId,
            command.tenantId,
            command.freezeOnly,
        )
        return commandHandler
            .closeAccountingPeriod(command)
            .also { periodCloseCounter.increment() }
    }

    @Transactional(TxType.REQUIRED)
    @Timed(value = "finance.command.fx_revaluation.duration", extraTags = ["command", "fxRevaluation"])
    @Counted(value = "finance.command.fx_revaluation.calls", extraTags = ["command", "fxRevaluation"])
    override fun runCurrencyRevaluation(command: RunCurrencyRevaluationCommand): JournalEntry? {
        LOGGER.debugf(
            "Running FX revaluation ledger=%s period=%s tenant=%s asOf=%s",
            command.ledgerId,
            command.accountingPeriodId,
            command.tenantId,
            command.asOf,
        )
        return commandHandler
            .runCurrencyRevaluation(command)
            ?.also { revaluationCounter.increment() }
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(FinanceCommandService::class.java)
    }
}
