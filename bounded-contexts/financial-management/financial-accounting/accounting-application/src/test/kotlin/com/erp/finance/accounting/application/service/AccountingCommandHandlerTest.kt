package com.erp.finance.accounting.application.service

import com.erp.finance.accounting.application.port.input.command.CloseAccountingPeriodCommand
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
import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.AccountingPeriodId
import com.erp.finance.accounting.domain.model.AccountingPeriodStatus
import com.erp.finance.accounting.domain.model.ChartOfAccounts
import com.erp.finance.accounting.domain.model.ChartOfAccountsId
import com.erp.finance.accounting.domain.model.EntryDirection
import com.erp.finance.accounting.domain.model.JournalEntry
import com.erp.finance.accounting.domain.model.JournalEntryStatus
import com.erp.finance.accounting.domain.model.Ledger
import com.erp.finance.accounting.domain.model.LedgerId
import com.erp.finance.accounting.domain.model.Money
import com.erp.finance.accounting.domain.policy.ExchangeRate
import com.erp.finance.accounting.domain.policy.ExchangeRateProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AccountingCommandHandlerTest {
    private lateinit var ledgerRepository: InMemoryLedgerRepository
    private lateinit var chartRepository: InMemoryChartRepository
    private lateinit var periodRepository: InMemoryAccountingPeriodRepository
    private lateinit var journalRepository: InMemoryJournalEntryRepository
    private lateinit var eventPublisher: RecordingFinanceEventPublisher
    private lateinit var exchangeRateProvider: StubExchangeRateProvider
    private lateinit var handler: AccountingCommandHandler

    @BeforeEach
    fun setUp() {
        ledgerRepository = InMemoryLedgerRepository()
        chartRepository = InMemoryChartRepository()
        periodRepository = InMemoryAccountingPeriodRepository()
        journalRepository = InMemoryJournalEntryRepository()
        eventPublisher = RecordingFinanceEventPublisher()
        exchangeRateProvider = StubExchangeRateProvider()
        handler =
            AccountingCommandHandler(
                ledgerRepository = ledgerRepository,
                chartRepository = chartRepository,
                periodRepository = periodRepository,
                journalRepository = journalRepository,
                eventPublisher = eventPublisher,
                exchangeRateProvider = exchangeRateProvider,
            )
    }

    @Test
    fun `postJournalEntry persists posted entry and emits event`() {
        val tenantId = UUID.randomUUID()
        val ledger =
            Ledger(
                id = LedgerId(),
                tenantId = tenantId,
                chartOfAccountsId = ChartOfAccountsId(),
                baseCurrency = "USD",
            )
        val period =
            AccountingPeriod(
                ledgerId = ledger.id,
                tenantId = tenantId,
                code = "2025-01",
                startDate = LocalDate.parse("2025-01-01"),
                endDate = LocalDate.parse("2025-01-31"),
            )
        ledgerRepository.given(ledger)
        periodRepository.given(period)

        val command =
            PostJournalEntryCommand(
                tenantId = tenantId,
                ledgerId = ledger.id.value,
                accountingPeriodId = period.id.value,
                reference = "JE-42",
                description = "Manual adjustment",
                bookedAt = Instant.parse("2025-01-15T00:00:00Z"),
                lines =
                    listOf(
                        JournalEntryLineCommand(
                            accountId = AccountId(),
                            direction = EntryDirection.DEBIT,
                            amount = Money(10_00),
                            currency = "usd",
                            description = "Cash",
                        ),
                        JournalEntryLineCommand(
                            accountId = AccountId(),
                            direction = EntryDirection.CREDIT,
                            amount = Money(10_00),
                            currency = "usd",
                            description = "Revenue",
                        ),
                    ),
            )

        val result = handler.postJournalEntry(command)

        assertEquals("JE-42", result.reference)
        assertEquals(2, result.lines.size)
        val directions = result.lines.map { it.direction }.toSet()
        assertTrue(directions.containsAll(setOf(EntryDirection.DEBIT, EntryDirection.CREDIT)))
        assertEquals(journalRepository.lastSaved?.id, result.id)
        assertSame(result, eventPublisher.journalEvents.single())
    }

    @Test
    fun `runCurrencyRevaluation posts adjustment entry for foreign currency lines`() {
        val tenantId = UUID.randomUUID()
        val ledger =
            Ledger(
                id = LedgerId(),
                tenantId = tenantId,
                chartOfAccountsId = ChartOfAccountsId(),
                baseCurrency = "USD",
            )
        val period =
            AccountingPeriod(
                ledgerId = ledger.id,
                tenantId = tenantId,
                code = "2025-03",
                startDate = LocalDate.parse("2025-03-01"),
                endDate = LocalDate.parse("2025-03-31"),
            )
        ledgerRepository.given(ledger)
        periodRepository.given(period)

        val assetAccount = AccountId(UUID.randomUUID())
        val offsetAccount = AccountId(UUID.randomUUID())
        val gainAccount = AccountId(UUID.randomUUID())
        val lossAccount = AccountId(UUID.randomUUID())

        exchangeRateProvider.stubRate("EUR", "USD", BigDecimal("1.00"))
        handler.postJournalEntry(
            PostJournalEntryCommand(
                tenantId = tenantId,
                ledgerId = ledger.id.value,
                accountingPeriodId = period.id.value,
                bookedAt = Instant.parse("2025-03-05T00:00:00Z"),
                lines =
                    listOf(
                        JournalEntryLineCommand(
                            accountId = assetAccount,
                            direction = EntryDirection.DEBIT,
                            amount = Money(1000),
                            currency = "EUR",
                            description = "Foreign receivable",
                        ),
                        JournalEntryLineCommand(
                            accountId = offsetAccount,
                            direction = EntryDirection.CREDIT,
                            amount = Money(1000),
                            currency = "USD",
                            description = "Revenue",
                        ),
                    ),
            ),
        )

        exchangeRateProvider.stubRate("EUR", "USD", BigDecimal("1.20"))

        val revaluation =
            handler.runCurrencyRevaluation(
                RunCurrencyRevaluationCommand(
                    tenantId = tenantId,
                    ledgerId = ledger.id.value,
                    accountingPeriodId = period.id.value,
                    asOf = Instant.parse("2025-03-31T23:59:59Z"),
                    bookedAt = Instant.parse("2025-03-31T23:59:59Z"),
                    gainAccountId = gainAccount,
                    lossAccountId = lossAccount,
                    reference = "FX-REV",
                ),
            )

        assertNotNull(revaluation)
        val assetAdjustment =
            revaluation!!.lines.first { it.accountId == assetAccount }
        val gainLine =
            revaluation.lines.first { it.accountId == gainAccount }
        assertEquals(EntryDirection.DEBIT, assetAdjustment.direction)
        assertEquals(EntryDirection.CREDIT, gainLine.direction)
        assertEquals(200L, assetAdjustment.amount.amount)
        assertEquals(200L, gainLine.amount.amount)
        assertEquals("USD", assetAdjustment.currency)
        assertEquals("USD", gainLine.currency)
        assertEquals(2, revaluation.lines.size)
    }

    @Test
    fun `closeAccountingPeriod transitions status and publishes change`() {
        val tenantId = UUID.randomUUID()
        val ledger =
            Ledger(
                id = LedgerId(),
                tenantId = tenantId,
                chartOfAccountsId = ChartOfAccountsId(),
                baseCurrency = "USD",
            )
        val openPeriod =
            AccountingPeriod(
                ledgerId = ledger.id,
                tenantId = tenantId,
                code = "2025-02",
                startDate = LocalDate.parse("2025-02-01"),
                endDate = LocalDate.parse("2025-02-28"),
                status = AccountingPeriodStatus.OPEN,
            )
        ledgerRepository.given(ledger)
        periodRepository.given(openPeriod)

        val command =
            CloseAccountingPeriodCommand(
                tenantId = tenantId,
                ledgerId = ledger.id.value,
                accountingPeriodId = openPeriod.id.value,
                freezeOnly = false,
            )

        val result = handler.closeAccountingPeriod(command)

        assertEquals(AccountingPeriodStatus.CLOSED, result.status)
        assertEquals(AccountingPeriodStatus.CLOSED, periodRepository.lastSaved?.status)
        val (emittedPeriod, previousStatus) = eventPublisher.periodEvents.single()
        assertEquals(openPeriod.id, emittedPeriod.id)
        assertEquals(AccountingPeriodStatus.OPEN, previousStatus)
    }

    @Test
    fun `defineAccount persists updated chart`() {
        val tenantId = UUID.randomUUID()
        val chart =
            ChartOfAccounts(
                id = ChartOfAccountsId(),
                tenantId = tenantId,
                baseCurrency = "USD",
                code = "FIN-GL",
                name = "Finance",
            )
        chartRepository.given(chart)

        val command =
            DefineAccountCommand(
                tenantId = tenantId,
                chartOfAccountsId = chart.id.value,
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
                currency = "usd",
                isPosting = true,
            )

        val updated = handler.defineAccount(command)

        assertEquals(1, updated.accounts.size)
        assertTrue(updated.accounts.values.any { it.code == "1000" })
        assertSame(updated, chartRepository.lastSaved)
    }

    @Test
    fun `postJournalEntry converts foreign currency lines`() {
        val tenantId = UUID.randomUUID()
        val ledger =
            Ledger(
                id = LedgerId(),
                tenantId = tenantId,
                chartOfAccountsId = ChartOfAccountsId(),
                baseCurrency = "USD",
            )
        val period =
            AccountingPeriod(
                ledgerId = ledger.id,
                tenantId = tenantId,
                code = "2025-02",
                startDate = LocalDate.parse("2025-02-01"),
                endDate = LocalDate.parse("2025-02-28"),
            )
        ledgerRepository.given(ledger)
        periodRepository.given(period)
        exchangeRateProvider.stubRate("EUR", "USD", BigDecimal("1.20"))

        val command =
            PostJournalEntryCommand(
                tenantId = tenantId,
                ledgerId = ledger.id.value,
                accountingPeriodId = period.id.value,
                lines =
                    listOf(
                        JournalEntryLineCommand(
                            accountId = AccountId(),
                            direction = EntryDirection.DEBIT,
                            amount = Money(10_00),
                            currency = "EUR",
                        ),
                        JournalEntryLineCommand(
                            accountId = AccountId(),
                            direction = EntryDirection.CREDIT,
                            amount = Money(12_00),
                            currency = "USD",
                        ),
                    ),
                bookedAt = Instant.parse("2025-02-10T00:00:00Z"),
            )

        val result = handler.postJournalEntry(command)

        val debitLine = result.lines.first { it.direction == EntryDirection.DEBIT }
        assertEquals("USD", debitLine.currency)
        assertEquals("EUR", debitLine.originalCurrency)
        assertEquals(12_00, debitLine.amount.amount)
        assertEquals(10_00, debitLine.originalAmount.amount)
    }
}

private class InMemoryLedgerRepository : LedgerRepository {
    private val storage = mutableMapOf<Pair<LedgerId, UUID>, Ledger>()

    var lastSaved: Ledger? = null
        private set

    fun given(ledger: Ledger) {
        storage[ledger.id to ledger.tenantId] = ledger
    }

    override fun save(ledger: Ledger): Ledger {
        storage[ledger.id to ledger.tenantId] = ledger
        lastSaved = ledger
        return ledger
    }

    override fun findById(
        id: LedgerId,
        tenantId: UUID,
    ): Ledger? = storage[id to tenantId]
}

private class InMemoryChartRepository : ChartOfAccountsRepository {
    private val storage = mutableMapOf<Pair<ChartOfAccountsId, UUID>, ChartOfAccounts>()
    var lastSaved: ChartOfAccounts? = null
        private set

    fun given(chart: ChartOfAccounts) {
        storage[chart.id to chart.tenantId] = chart
    }

    override fun save(chartOfAccounts: ChartOfAccounts): ChartOfAccounts {
        storage[chartOfAccounts.id to chartOfAccounts.tenantId] = chartOfAccounts
        lastSaved = chartOfAccounts
        return chartOfAccounts
    }

    override fun findById(
        id: ChartOfAccountsId,
        tenantId: UUID,
    ): ChartOfAccounts? = storage[id to tenantId]
}

private class InMemoryAccountingPeriodRepository : AccountingPeriodRepository {
    private val storage = mutableMapOf<Pair<AccountingPeriodId, UUID>, AccountingPeriod>()
    var lastSaved: AccountingPeriod? = null
        private set

    fun given(period: AccountingPeriod) {
        storage[period.id to period.tenantId] = period
    }

    override fun save(period: AccountingPeriod): AccountingPeriod {
        storage[period.id to period.tenantId] = period
        lastSaved = period
        return period
    }

    override fun findById(
        id: AccountingPeriodId,
        tenantId: UUID,
    ): AccountingPeriod? = storage[id to tenantId]

    override fun findOpenByLedger(
        ledgerId: LedgerId,
        tenantId: UUID,
    ): List<AccountingPeriod> =
        storage.values.filter {
            it.tenantId == tenantId &&
                it.ledgerId == ledgerId &&
                it.status == AccountingPeriodStatus.OPEN
        }
}

private class InMemoryJournalEntryRepository : JournalEntryRepository {
    private val storage = mutableMapOf<Pair<UUID, UUID>, JournalEntry>()
    var lastSaved: JournalEntry? = null
        private set

    override fun save(entry: JournalEntry): JournalEntry {
        storage[entry.id to entry.tenantId] = entry
        lastSaved = entry
        return entry
    }

    override fun findById(
        id: UUID,
        tenantId: UUID,
    ): JournalEntry? = storage[id to tenantId]

    override fun findPostedByLedgerAndPeriod(
        ledgerId: LedgerId,
        accountingPeriodId: AccountingPeriodId,
        tenantId: UUID,
    ): List<JournalEntry> =
        storage
            .values
            .filter {
                it.tenantId == tenantId &&
                    it.ledgerId == ledgerId &&
                    it.accountingPeriodId == accountingPeriodId &&
                    it.status == JournalEntryStatus.POSTED
            }
}

private class RecordingFinanceEventPublisher : FinanceEventPublisher {
    val journalEvents: MutableList<JournalEntry> = mutableListOf()
    val periodEvents: MutableList<Pair<AccountingPeriod, AccountingPeriodStatus>> = mutableListOf()

    override fun publishJournalPosted(entry: JournalEntry) {
        journalEvents += entry
    }

    override fun publishPeriodUpdated(
        period: AccountingPeriod,
        previousStatus: AccountingPeriodStatus,
    ) {
        periodEvents += period to previousStatus
    }
}

private class StubExchangeRateProvider : ExchangeRateProvider {
    private val rates = mutableMapOf<Pair<String, String>, ExchangeRate>()

    fun stubRate(
        base: String,
        quote: String,
        rate: BigDecimal,
    ) {
        rates[base.uppercase() to quote.uppercase()] =
            ExchangeRate(
                baseCurrency = base.uppercase(),
                quoteCurrency = quote.uppercase(),
                rate = rate,
                asOf = Instant.now(),
            )
    }

    override fun findRate(
        baseCurrency: String,
        quoteCurrency: String,
        asOf: Instant,
    ): ExchangeRate? =
        if (baseCurrency.uppercase() == quoteCurrency.uppercase()) {
            ExchangeRate(baseCurrency.uppercase(), quoteCurrency.uppercase(), BigDecimal.ONE, asOf)
        } else {
            rates[baseCurrency.uppercase() to quoteCurrency.uppercase()]
        }
}
