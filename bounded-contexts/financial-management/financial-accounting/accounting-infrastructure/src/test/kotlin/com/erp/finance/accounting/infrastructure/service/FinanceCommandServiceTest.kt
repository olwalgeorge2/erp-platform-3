package com.erp.finance.accounting.infrastructure.service

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
import com.erp.finance.accounting.application.service.AccountingCommandHandler
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
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

class FinanceCommandServiceTest {
    private lateinit var ledgerRepository: InMemoryLedgerRepository
    private lateinit var chartRepository: InMemoryChartRepository
    private lateinit var periodRepository: InMemoryAccountingPeriodRepository
    private lateinit var journalRepository: InMemoryJournalEntryRepository
    private lateinit var eventPublisher: RecordingFinanceEventPublisher
    private lateinit var meterRegistry: SimpleMeterRegistry
    private lateinit var exchangeRateProvider: StubExchangeRateProvider
    private lateinit var service: FinanceCommandService

    private val tenantId: UUID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")

    @BeforeEach
    fun setUp() {
        ledgerRepository = InMemoryLedgerRepository()
        chartRepository = InMemoryChartRepository()
        periodRepository = InMemoryAccountingPeriodRepository()
        journalRepository = InMemoryJournalEntryRepository()
        eventPublisher = RecordingFinanceEventPublisher()
        meterRegistry = SimpleMeterRegistry()
        exchangeRateProvider = StubExchangeRateProvider()
        val handler =
            AccountingCommandHandler(
                ledgerRepository = ledgerRepository,
                chartRepository = chartRepository,
                periodRepository = periodRepository,
                journalRepository = journalRepository,
                eventPublisher = eventPublisher,
                exchangeRateProvider = exchangeRateProvider,
            )
        service = FinanceCommandService(handler, meterRegistry)
    }

    @Test
    fun `createLedger delegates to handler`() {
        val command =
            CreateLedgerCommand(
                tenantId = tenantId,
                chartOfAccountsId = UUID.randomUUID(),
                baseCurrency = "usd",
                chartCode = "FIN",
                chartName = "Finance",
            )

        val ledger = service.createLedger(command)

        assertEquals("USD", ledger.baseCurrency)
        assertEquals(tenantId, ledger.tenantId)
        assertTrue(ledgerRepository.findById(ledger.id, tenantId) != null)
    }

    @Test
    fun `defineAccount updates chart`() {
        val chart =
            ChartOfAccounts(
                id = ChartOfAccountsId(UUID.randomUUID()),
                tenantId = tenantId,
                baseCurrency = "USD",
                code = "FIN",
                name = "Finance",
            )
        chartRepository.save(chart)
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

        val updated = service.defineAccount(command)

        assertEquals(1, updated.accounts.size)
        assertEquals(
            "1000",
            updated.accounts.values
                .first()
                .code,
        )
        assertSame(updated, chartRepository.findById(chart.id, tenantId))
    }

    @Test
    fun `postJournalEntry persists entry and emits event`() {
        val ledger =
            Ledger(
                id = LedgerId(UUID.randomUUID()),
                tenantId = tenantId,
                chartOfAccountsId = ChartOfAccountsId(UUID.randomUUID()),
                baseCurrency = "USD",
            )
        ledgerRepository.save(ledger)
        val period =
            AccountingPeriod(
                id = AccountingPeriodId(UUID.randomUUID()),
                ledgerId = ledger.id,
                tenantId = tenantId,
                code = "2025-01",
                startDate = LocalDate.parse("2025-01-01"),
                endDate = LocalDate.parse("2025-01-31"),
            )
        periodRepository.save(period)
        val command =
            PostJournalEntryCommand(
                tenantId = tenantId,
                ledgerId = ledger.id.value,
                accountingPeriodId = period.id.value,
                reference = "JE-123",
                description = "Test entry",
                bookedAt = Instant.parse("2025-01-15T00:00:00Z"),
                lines =
                    listOf(
                        JournalEntryLineCommand(
                            accountId = AccountId(),
                            direction = EntryDirection.DEBIT,
                            amount = Money(1_00),
                            currency = "usd",
                            description = "Cash",
                        ),
                        JournalEntryLineCommand(
                            accountId = AccountId(),
                            direction = EntryDirection.CREDIT,
                            amount = Money(1_00),
                            currency = "usd",
                            description = "Revenue",
                        ),
                    ),
            )

        val entry = service.postJournalEntry(command)

        assertEquals(JournalEntryStatus.POSTED.name, entry.status.name)
        assertSame(entry.id, journalRepository.findById(entry.id, tenantId)?.id)
        assertEquals(entry.id, eventPublisher.journalEvents.single().id)
    }

    @Test
    fun `closePeriod transitions status and emits event`() {
        val ledger =
            Ledger(
                id = LedgerId(UUID.randomUUID()),
                tenantId = tenantId,
                chartOfAccountsId = ChartOfAccountsId(UUID.randomUUID()),
                baseCurrency = "USD",
            )
        ledgerRepository.save(ledger)
        val period =
            AccountingPeriod(
                id = AccountingPeriodId(UUID.randomUUID()),
                ledgerId = ledger.id,
                tenantId = tenantId,
                code = "2025-02",
                startDate = LocalDate.parse("2025-02-01"),
                endDate = LocalDate.parse("2025-02-28"),
                status = AccountingPeriodStatus.OPEN,
            )
        periodRepository.save(period)

        val result =
            service.closePeriod(
                CloseAccountingPeriodCommand(
                    tenantId = tenantId,
                    ledgerId = ledger.id.value,
                    accountingPeriodId = period.id.value,
                    freezeOnly = false,
                ),
            )

        assertEquals(AccountingPeriodStatus.CLOSED, result.status)
        val event = eventPublisher.periodEvents.single()
        assertEquals(result.id, event.first.id)
        assertEquals(AccountingPeriodStatus.OPEN, event.second)
    }

    @Test
    fun `runCurrencyRevaluation posts adjustment when foreign exposure exists`() {
        val ledger =
            Ledger(
                id = LedgerId(UUID.randomUUID()),
                tenantId = tenantId,
                chartOfAccountsId = ChartOfAccountsId(UUID.randomUUID()),
                baseCurrency = "USD",
            )
        ledgerRepository.save(ledger)
        val period =
            AccountingPeriod(
                id = AccountingPeriodId(UUID.randomUUID()),
                ledgerId = ledger.id,
                tenantId = tenantId,
                code = "2025-04",
                startDate = LocalDate.parse("2025-04-01"),
                endDate = LocalDate.parse("2025-04-30"),
                status = AccountingPeriodStatus.OPEN,
            )
        periodRepository.save(period)

        val assetAccount = AccountId(UUID.randomUUID())
        val creditAccount = AccountId(UUID.randomUUID())
        val gainAccount = AccountId(UUID.randomUUID())
        val lossAccount = AccountId(UUID.randomUUID())

        exchangeRateProvider.stubRate("EUR", "USD", BigDecimal("1.00"))
        service.postJournalEntry(
            PostJournalEntryCommand(
                tenantId = tenantId,
                ledgerId = ledger.id.value,
                accountingPeriodId = period.id.value,
                bookedAt = Instant.parse("2025-04-15T00:00:00Z"),
                lines =
                    listOf(
                        JournalEntryLineCommand(
                            accountId = assetAccount,
                            direction = EntryDirection.DEBIT,
                            amount = Money(2000),
                            currency = "EUR",
                        ),
                        JournalEntryLineCommand(
                            accountId = creditAccount,
                            direction = EntryDirection.CREDIT,
                            amount = Money(2000),
                            currency = "USD",
                        ),
                    ),
            ),
        )

        exchangeRateProvider.stubRate("EUR", "USD", BigDecimal("1.10"))

        val result =
            service.runCurrencyRevaluation(
                RunCurrencyRevaluationCommand(
                    tenantId = tenantId,
                    ledgerId = ledger.id.value,
                    accountingPeriodId = period.id.value,
                    asOf = Instant.parse("2025-04-30T23:59:59Z"),
                    bookedAt = Instant.parse("2025-04-30T23:59:59Z"),
                    gainAccountId = gainAccount,
                    lossAccountId = lossAccount,
                ),
            )

        assertNotNull(result)
        val assetAdjustment = result!!.lines.first { it.accountId == assetAccount }
        val gainLine = result.lines.first { it.accountId == gainAccount }
        assertEquals(EntryDirection.DEBIT, assetAdjustment.direction)
        assertEquals(EntryDirection.CREDIT, gainLine.direction)
        assertEquals(200L, assetAdjustment.amount.amount)
        assertEquals(200L, gainLine.amount.amount)
        assertEquals(2, result.lines.size)
        assertEquals(2, eventPublisher.journalEvents.size)
        assertEquals(
            1.0,
            meterRegistry
                .get("finance.command.fx_revaluation.total")
                .counter()
                .count(),
        )
    }
}

private class InMemoryLedgerRepository : LedgerRepository {
    private val storage = mutableMapOf<Pair<LedgerId, UUID>, Ledger>()

    override fun save(ledger: Ledger): Ledger {
        storage[ledger.id to ledger.tenantId] = ledger
        return ledger
    }

    override fun findById(
        id: LedgerId,
        tenantId: UUID,
    ): Ledger? = storage[id to tenantId]
}

private class InMemoryChartRepository : ChartOfAccountsRepository {
    private val storage = mutableMapOf<Pair<ChartOfAccountsId, UUID>, ChartOfAccounts>()

    override fun save(chartOfAccounts: ChartOfAccounts): ChartOfAccounts {
        storage[chartOfAccounts.id to chartOfAccounts.tenantId] = chartOfAccounts
        return chartOfAccounts
    }

    override fun findById(
        id: ChartOfAccountsId,
        tenantId: UUID,
    ): ChartOfAccounts? = storage[id to tenantId]
}

private class InMemoryAccountingPeriodRepository : AccountingPeriodRepository {
    private val storage = mutableMapOf<Pair<AccountingPeriodId, UUID>, AccountingPeriod>()

    override fun save(period: AccountingPeriod): AccountingPeriod {
        storage[period.id to period.tenantId] = period
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

    override fun save(entry: JournalEntry): JournalEntry {
        storage[entry.id to entry.tenantId] = entry
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
    private val rates = mutableMapOf<Pair<String, String>, BigDecimal>()

    fun stubRate(
        baseCurrency: String,
        quoteCurrency: String,
        rate: BigDecimal,
    ) {
        rates[baseCurrency.uppercase() to quoteCurrency.uppercase()] = rate
    }

    override fun findRate(
        baseCurrency: String,
        quoteCurrency: String,
        asOf: Instant,
    ): ExchangeRate {
        val base = baseCurrency.uppercase()
        val quote = quoteCurrency.uppercase()
        val resolved =
            when {
                base == quote -> BigDecimal.ONE
                else ->
                    rates[base to quote]
                        ?: error("Missing exchange rate from $base to $quote")
            }
        return ExchangeRate(
            baseCurrency = base,
            quoteCurrency = quote,
            rate = resolved,
            asOf = asOf,
        )
    }
}
