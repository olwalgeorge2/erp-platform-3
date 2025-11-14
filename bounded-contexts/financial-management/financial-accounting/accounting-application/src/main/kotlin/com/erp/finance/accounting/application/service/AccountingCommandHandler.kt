package com.erp.finance.accounting.application.service

import com.erp.finance.accounting.application.port.input.command.CloseAccountingPeriodCommand
import com.erp.finance.accounting.application.port.input.command.CreateLedgerCommand
import com.erp.finance.accounting.application.port.input.command.DefineAccountCommand
import com.erp.finance.accounting.application.port.input.command.PostJournalEntryCommand
import com.erp.finance.accounting.application.port.output.AccountingPeriodRepository
import com.erp.finance.accounting.application.port.output.ChartOfAccountsRepository
import com.erp.finance.accounting.application.port.output.FinanceEventPublisher
import com.erp.finance.accounting.application.port.output.JournalEntryRepository
import com.erp.finance.accounting.application.port.output.LedgerRepository
import com.erp.finance.accounting.domain.model.AccountId
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.AccountingPeriodId
import com.erp.finance.accounting.domain.model.ChartOfAccounts
import com.erp.finance.accounting.domain.model.ChartOfAccountsId
import com.erp.finance.accounting.domain.model.EntryDirection
import com.erp.finance.accounting.domain.model.JournalEntry
import com.erp.finance.accounting.domain.model.JournalEntryLine
import com.erp.finance.accounting.domain.model.Ledger
import com.erp.finance.accounting.domain.model.LedgerId
import com.erp.finance.accounting.domain.model.Money

class AccountingCommandHandler(
    private val ledgerRepository: LedgerRepository,
    private val chartRepository: ChartOfAccountsRepository,
    private val periodRepository: AccountingPeriodRepository,
    private val journalRepository: JournalEntryRepository,
    private val eventPublisher: FinanceEventPublisher,
) {
    fun createLedger(command: CreateLedgerCommand): Ledger {
        val baseCurrency = command.baseCurrency.uppercase()
        require(baseCurrency.length == 3) { "Currency must be ISO-4217 3-letter code" }
        val chartId = ChartOfAccountsId(command.chartOfAccountsId)

        val chart =
            chartRepository.findById(chartId, command.tenantId)
                ?: chartRepository.save(
                    ChartOfAccounts(
                        id = chartId,
                        tenantId = command.tenantId,
                        baseCurrency = baseCurrency,
                        code = command.chartCode,
                        name = command.chartName,
                    ),
                )

        val ledger =
            Ledger(
                tenantId = command.tenantId,
                chartOfAccountsId = chart.id,
                baseCurrency = baseCurrency,
            )
        return ledgerRepository.save(ledger)
    }

    fun defineAccount(command: DefineAccountCommand): ChartOfAccounts {
        val chartId = ChartOfAccountsId(command.chartOfAccountsId)
        val chart =
            chartRepository.findById(chartId, command.tenantId)
                ?: error("Chart of accounts not found")

        val updated =
            chart.defineAccount(
                code = command.code,
                name = command.name,
                type = command.type,
                currency = command.currency ?: chart.baseCurrency,
                parentAccountId = command.parentAccountId?.let { AccountId(it) },
                isPosting = command.isPosting,
            )

        return chartRepository.save(updated)
    }

    fun postJournalEntry(command: PostJournalEntryCommand): JournalEntry {
        val ledgerId = LedgerId(command.ledgerId)
        val periodId = AccountingPeriodId(command.accountingPeriodId)

        val ledger =
            ledgerRepository.findById(ledgerId, command.tenantId)
                ?: error("Ledger not found")

        val period =
            periodRepository.findById(periodId, command.tenantId)
                ?: error("Accounting period not found")

        require(
            command.lines.any { it.direction == EntryDirection.DEBIT } &&
                command.lines.any { it.direction == EntryDirection.CREDIT },
        ) {
            "Journal entry requires both debit and credit lines"
        }

        val balanceCheck =
            command.lines.fold(Money.ZERO to Money.ZERO) { acc, line ->
                val (debits, credits) = acc
                if (line.direction == EntryDirection.DEBIT) {
                    (debits + line.amount) to credits
                } else {
                    debits to (credits + line.amount)
                }
            }
        require(balanceCheck.first.amount == balanceCheck.second.amount) {
            "Debits and credits must balance"
        }

        val entry =
            JournalEntry
                .draft(
                    tenantId = command.tenantId,
                    ledgerId = ledgerId,
                    periodId = periodId,
                    lines =
                        command.lines.map {
                            JournalEntryLine(
                                accountId = it.accountId,
                                direction = it.direction,
                                amount = it.amount,
                                currency = (it.currency ?: ledger.baseCurrency).uppercase(),
                                description = it.description,
                            )
                        },
                    reference = command.reference,
                    description = command.description,
                    bookedAt = command.bookedAt,
                ).post()

        val saved = journalRepository.save(entry)
        eventPublisher.publishJournalPosted(saved)
        return saved
    }

    fun closeAccountingPeriod(command: CloseAccountingPeriodCommand): AccountingPeriod {
        val ledgerId = LedgerId(command.ledgerId)
        val periodId = AccountingPeriodId(command.accountingPeriodId)

        ledgerRepository.findById(ledgerId, command.tenantId)
            ?: error("Ledger not found")

        val period =
            periodRepository.findById(periodId, command.tenantId)
                ?: error("Period not found")

        val next =
            if (command.freezeOnly) {
                period.freeze()
            } else {
                period.close()
            }

        if (next.status == period.status) {
            return period
        }

        val saved = periodRepository.save(next)
        eventPublisher.publishPeriodUpdated(saved, period.status)
        return saved
    }
}
