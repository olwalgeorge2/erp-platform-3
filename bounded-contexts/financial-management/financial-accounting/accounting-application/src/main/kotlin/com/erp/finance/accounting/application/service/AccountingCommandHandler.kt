package com.erp.finance.accounting.application.service

import com.erp.finance.accounting.application.port.input.command.CloseAccountingPeriodCommand
import com.erp.finance.accounting.application.port.input.command.CreateLedgerCommand
import com.erp.finance.accounting.application.port.input.command.DefineAccountCommand
import com.erp.finance.accounting.application.port.input.command.JournalEntryLineCommand
import com.erp.finance.accounting.application.port.input.command.PostJournalEntryCommand
import com.erp.finance.accounting.application.port.input.command.RunCurrencyRevaluationCommand
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
import com.erp.finance.accounting.domain.policy.ExchangeRateProvider
import java.time.Instant
import java.util.UUID

class AccountingCommandHandler(
    private val ledgerRepository: LedgerRepository,
    private val chartRepository: ChartOfAccountsRepository,
    private val periodRepository: AccountingPeriodRepository,
    private val journalRepository: JournalEntryRepository,
    private val eventPublisher: FinanceEventPublisher,
    private val exchangeRateProvider: ExchangeRateProvider,
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

        val convertedLines =
            command.lines.map { line ->
                convertLine(
                    line = line,
                    ledgerCurrency = ledger.baseCurrency,
                    bookedAt = command.bookedAt,
                )
            }
        val balanceCheck =
            convertedLines.fold(Money.ZERO to Money.ZERO) { acc, line ->
                val (debits, credits) = acc
                if (line.direction == EntryDirection.DEBIT) {
                    (debits + line.amount) to credits
                } else {
                    debits to (credits + line.amount)
                }
            }
        require(balanceCheck.first.amount == balanceCheck.second.amount) {
            "Debits and credits must balance after FX conversion"
        }

        val entry =
            JournalEntry
                .draft(
                    tenantId = command.tenantId,
                    ledgerId = ledgerId,
                    periodId = periodId,
                    lines = convertedLines,
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

    fun runCurrencyRevaluation(command: RunCurrencyRevaluationCommand): JournalEntry? {
        val ledgerId = LedgerId(command.ledgerId)
        val periodId = AccountingPeriodId(command.accountingPeriodId)

        val ledger =
            ledgerRepository.findById(ledgerId, command.tenantId)
                ?: error("Ledger not found")
        periodRepository.findById(periodId, command.tenantId)
            ?: error("Accounting period not found")

        val ledgerCurrency = ledger.baseCurrency.uppercase()
        val postedEntries =
            journalRepository
                .findPostedByLedgerAndPeriod(ledgerId, periodId, command.tenantId)
                .ifEmpty { return null }

        val adjustments = mutableMapOf<Pair<AccountId, EntryDirection>, Long>()
        postedEntries.forEach { entry ->
            entry.lines
                .filter { it.originalCurrency.uppercase() != ledgerCurrency }
                .forEach { line ->
                    val rate =
                        exchangeRateProvider.findRate(
                            baseCurrency = line.originalCurrency,
                            quoteCurrency = ledgerCurrency,
                            asOf = command.asOf,
                        )
                            ?: error(
                                "Missing exchange rate from ${line.originalCurrency} to $ledgerCurrency for revaluation",
                            )
                    val revalued = rate.convert(line.originalAmount)
                    val delta = revalued.amount - line.amount.amount
                    if (delta == 0L) {
                        return@forEach
                    }
                    if (line.direction == EntryDirection.DEBIT) {
                        if (delta > 0) {
                            recordAdjustment(adjustments, line.accountId, EntryDirection.DEBIT, delta)
                            recordAdjustment(
                                adjustments,
                                command.gainAccountId,
                                EntryDirection.CREDIT,
                                delta,
                            )
                        } else {
                            val difference = kotlin.math.abs(delta)
                            recordAdjustment(
                                adjustments,
                                line.accountId,
                                EntryDirection.CREDIT,
                                difference,
                            )
                            recordAdjustment(
                                adjustments,
                                command.lossAccountId,
                                EntryDirection.DEBIT,
                                difference,
                            )
                        }
                    } else {
                        if (delta > 0) {
                            recordAdjustment(
                                adjustments,
                                line.accountId,
                                EntryDirection.CREDIT,
                                delta,
                            )
                            recordAdjustment(
                                adjustments,
                                command.lossAccountId,
                                EntryDirection.DEBIT,
                                delta,
                            )
                        } else {
                            val difference = kotlin.math.abs(delta)
                            recordAdjustment(
                                adjustments,
                                line.accountId,
                                EntryDirection.DEBIT,
                                difference,
                            )
                            recordAdjustment(
                                adjustments,
                                command.gainAccountId,
                                EntryDirection.CREDIT,
                                difference,
                            )
                        }
                    }
                }
        }

        if (adjustments.isEmpty()) {
            return null
        }

        val description =
            command.description
                ?: "FX revaluation as of ${command.asOf}"
        val lines =
            adjustments.entries.map { (key, amount) ->
                JournalEntryLine(
                    accountId = key.first,
                    direction = key.second,
                    amount = Money(amount),
                    currency = ledgerCurrency,
                    description = description,
                    originalCurrency = ledgerCurrency,
                    originalAmount = Money(amount),
                )
            }

        require(lines.size >= 2) { "Revaluation entry must produce at least two lines" }

        val entry =
            JournalEntry
                .draft(
                    tenantId = command.tenantId,
                    ledgerId = ledgerId,
                    periodId = periodId,
                    lines = lines,
                    reference = command.reference ?: defaultRevaluationReference(command.asOf, ledgerId.value),
                    description = description,
                    bookedAt = command.bookedAt,
                ).post(command.bookedAt)

        val saved = journalRepository.save(entry)
        eventPublisher.publishJournalPosted(saved)
        return saved
    }

    private fun recordAdjustment(
        adjustments: MutableMap<Pair<AccountId, EntryDirection>, Long>,
        accountId: AccountId,
        direction: EntryDirection,
        amount: Long,
    ) {
        if (amount <= 0) {
            return
        }
        val key = accountId to direction
        adjustments[key] = (adjustments[key] ?: 0L) + amount
    }

    private fun defaultRevaluationReference(
        asOf: Instant,
        ledgerUuid: UUID,
    ): String = "FX-REV-${ledgerUuid.toString().take(8)}-${asOf.toEpochMilli()}"

    private fun convertLine(
        line: JournalEntryLineCommand,
        ledgerCurrency: String,
        bookedAt: Instant,
    ): JournalEntryLine {
        val targetCurrency = ledgerCurrency.uppercase()
        val sourceCurrency = (line.currency ?: targetCurrency).uppercase()
        val sourceAmount = line.amount

        val convertedAmount =
            if (sourceCurrency == targetCurrency) {
                sourceAmount
            } else {
                val rate =
                    exchangeRateProvider.findRate(
                        baseCurrency = sourceCurrency,
                        quoteCurrency = targetCurrency,
                        asOf = bookedAt,
                    ) ?: error("Missing exchange rate from $sourceCurrency to $targetCurrency")
                rate.convert(sourceAmount)
            }

        return JournalEntryLine(
            accountId = line.accountId,
            direction = line.direction,
            amount = convertedAmount,
            currency = targetCurrency,
            description = line.description,
            originalCurrency = sourceCurrency,
            originalAmount = sourceAmount,
        )
    }
}
