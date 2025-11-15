package com.erp.finance.accounting.infrastructure.adapter.input.rest

import com.erp.finance.accounting.application.port.input.FinanceCommandUseCase
import com.erp.finance.accounting.application.port.input.command.CloseAccountingPeriodCommand
import com.erp.finance.accounting.application.port.input.command.CreateLedgerCommand
import com.erp.finance.accounting.application.port.input.command.DefineAccountCommand
import com.erp.finance.accounting.application.port.input.command.PostJournalEntryCommand
import com.erp.finance.accounting.application.port.input.command.RunCurrencyRevaluationCommand
import com.erp.finance.accounting.domain.model.AccountId
import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.AccountingPeriodId
import com.erp.finance.accounting.domain.model.AccountingPeriodStatus
import com.erp.finance.accounting.domain.model.ChartOfAccounts
import com.erp.finance.accounting.domain.model.ChartOfAccountsId
import com.erp.finance.accounting.domain.model.EntryDirection
import com.erp.finance.accounting.domain.model.JournalEntry
import com.erp.finance.accounting.domain.model.JournalEntryLine
import com.erp.finance.accounting.domain.model.Ledger
import com.erp.finance.accounting.domain.model.LedgerId
import com.erp.finance.accounting.domain.model.Money
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.AccountingPeriodResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.ChartOfAccountsResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.ChartPathParams
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.ClosePeriodRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.CreateLedgerRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.DefineAccountRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.JournalEntryResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.LedgerPeriodPathParams
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.LedgerResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.PostJournalEntryLineRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.PostJournalEntryRequest
import com.erp.financial.shared.api.ErrorResponse
import com.erp.financial.shared.validation.FinanceValidationException
import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class FinanceCommandResourceTest {
    private val useCase = FakeFinanceCommandUseCase()
    private val resource = FinanceCommandResource(useCase)

    @Test
    fun `createLedger returns 201 with payload`() {
        val request =
            CreateLedgerRequest(
                tenantId = TENANT_ID,
                chartOfAccountsId = UUID.randomUUID(),
                baseCurrency = "usd",
                chartCode = "FIN",
                chartName = "Finance",
            )
        val ledger =
            Ledger(
                id = LedgerId(),
                tenantId = TENANT_ID,
                chartOfAccountsId = ChartOfAccountsId(),
                baseCurrency = "USD",
            )
        useCase.createLedgerHandler = { ledger }

        val response = resource.createLedger(request)

        assertStatus(Response.Status.CREATED, response)
        val payload = response.entity as LedgerResponse
        assertEquals(ledger.id.value, payload.id)
        assertEquals("USD", payload.baseCurrency)
        assertSame(request.chartOfAccountsId, useCase.lastCreateLedger?.chartOfAccountsId)
    }

    @Test
    fun `defineAccount returns 200 and uses chart param`() {
        val chartId = UUID.randomUUID()
        val chart =
            ChartOfAccounts(
                id = ChartOfAccountsId(chartId),
                tenantId = TENANT_ID,
                baseCurrency = "USD",
                code = "FIN",
                name = "Finance",
            ).defineAccount(
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
            )
        useCase.defineAccountHandler = { chart }
        val request =
            DefineAccountRequest(
                tenantId = TENANT_ID,
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
                currency = "usd",
            )

        val response =
            resource.defineAccount(
                ChartPathParams(chartId = chartId.toString()),
                request,
            )

        assertStatus(Response.Status.OK, response)
        val payload = response.entity as ChartOfAccountsResponse
        assertEquals(chart.id.value, payload.id)
        assertEquals(1, payload.accounts.size)
        assertEquals(chartId, useCase.lastDefineAccount?.chartOfAccountsId)
    }

    @Test
    fun `postJournalEntry returns 202`() {
        val ledgerId = UUID.randomUUID()
        val periodId = UUID.randomUUID()
        val journal =
            JournalEntry
                .draft(
                    tenantId = TENANT_ID,
                    ledgerId = LedgerId(ledgerId),
                    periodId = AccountingPeriodId(periodId),
                    lines =
                        listOf(
                            JournalEntryLine(
                                accountId = AccountId(),
                                direction = EntryDirection.DEBIT,
                                amount = Money(1_00),
                                currency = "USD",
                            ),
                            JournalEntryLine(
                                accountId = AccountId(),
                                direction = EntryDirection.CREDIT,
                                amount = Money(1_00),
                                currency = "USD",
                            ),
                        ),
                    reference = "JE-9",
                    bookedAt = Instant.parse("2025-01-15T00:00:00Z"),
                ).post()
        useCase.postJournalEntryHandler = { journal }
        val request =
            PostJournalEntryRequest(
                tenantId = TENANT_ID,
                ledgerId = ledgerId,
                accountingPeriodId = periodId,
                lines =
                    listOf(
                        PostJournalEntryLineRequest(AccountId().value, EntryDirection.DEBIT, 1_00, "usd"),
                        PostJournalEntryLineRequest(AccountId().value, EntryDirection.CREDIT, 1_00, "usd"),
                    ),
            )

        val response = resource.postJournalEntry(request)

        assertStatus(Response.Status.ACCEPTED, response)
        val payload = response.entity as JournalEntryResponse
        assertEquals(journal.id, payload.id)
        val directions = payload.lines.map { it.direction }.toSet()
        assertTrue(directions.containsAll(setOf(EntryDirection.DEBIT, EntryDirection.CREDIT)))
        assertEquals(ledgerId, useCase.lastPostJournalEntry?.ledgerId)
    }

    @Test
    fun `closePeriod returns 200`() {
        val ledgerId = UUID.randomUUID()
        val periodId = UUID.randomUUID()
        val period =
            AccountingPeriod(
                id = AccountingPeriodId(periodId),
                ledgerId = LedgerId(ledgerId),
                tenantId = TENANT_ID,
                code = "2025-02",
                startDate = LocalDate.parse("2025-02-01"),
                endDate = LocalDate.parse("2025-02-28"),
                status = AccountingPeriodStatus.CLOSED,
            )
        useCase.closePeriodHandler = { period }
        val response =
            resource.closePeriod(
                LedgerPeriodPathParams(
                    ledgerId = ledgerId.toString(),
                    periodId = periodId.toString(),
                ),
                ClosePeriodRequest(
                    tenantId = TENANT_ID,
                    freezeOnly = false,
                ),
            )

        assertStatus(Response.Status.OK, response)
        val payload = response.entity as AccountingPeriodResponse
        assertEquals(period.id.value, payload.id)
        assertEquals(ledgerId, useCase.lastClosePeriod?.ledgerId)
    }

    @Test
    fun `postJournalEntry enforces minimum line count`() {
        val request =
            PostJournalEntryRequest(
                tenantId = TENANT_ID,
                ledgerId = UUID.randomUUID(),
                accountingPeriodId = UUID.randomUUID(),
                lines =
                    listOf(
                        PostJournalEntryLineRequest(
                            accountId = UUID.randomUUID(),
                            direction = EntryDirection.DEBIT,
                            amountMinor = 100,
                        ),
                    ),
            )

        assertThrows(FinanceValidationException::class.java) {
            resource.postJournalEntry(request)
        }
        assertNull(useCase.lastPostJournalEntry)
    }

    @Test
    fun `illegal argument maps to bad request`() {
        useCase.createLedgerHandler = { throw IllegalArgumentException("invalid") }
        val request =
            CreateLedgerRequest(
                tenantId = TENANT_ID,
                chartOfAccountsId = UUID.randomUUID(),
                baseCurrency = "usd",
            )

        val response = resource.createLedger(request)

        assertStatus(Response.Status.BAD_REQUEST, response)
        val error = response.entity as ErrorResponse
        assertEquals("INVALID_REQUEST", error.code)
        assertNotNull(error.message)
    }

    @Test
    fun `illegal state maps to not found`() {
        useCase.createLedgerHandler = { throw IllegalStateException("missing") }
        val request =
            CreateLedgerRequest(
                tenantId = TENANT_ID,
                chartOfAccountsId = UUID.randomUUID(),
                baseCurrency = "usd",
            )

        val response = resource.createLedger(request)

        assertStatus(Response.Status.NOT_FOUND, response)
        val error = response.entity as ErrorResponse
        assertEquals("RESOURCE_NOT_FOUND", error.code)
    }

    @Test
    fun `unexpected errors return 500`() {
        useCase.createLedgerHandler = { throw RuntimeException("boom") }
        val request =
            CreateLedgerRequest(
                tenantId = TENANT_ID,
                chartOfAccountsId = UUID.randomUUID(),
                baseCurrency = "usd",
            )

        val response = resource.createLedger(request)

        assertStatus(Response.Status.INTERNAL_SERVER_ERROR, response)
        val error = response.entity as ErrorResponse
        assertEquals("FINANCE_COMMAND_ERROR", error.code)
    }

    private fun assertStatus(
        status: Response.Status,
        response: Response,
    ) {
        assertEquals(status.statusCode, response.status)
    }

    private class FakeFinanceCommandUseCase : FinanceCommandUseCase {
        var lastCreateLedger: CreateLedgerCommand? = null
            private set
        var lastDefineAccount: DefineAccountCommand? = null
            private set
        var lastPostJournalEntry: PostJournalEntryCommand? = null
            private set
        var lastClosePeriod: CloseAccountingPeriodCommand? = null
            private set
        var lastRevaluation: RunCurrencyRevaluationCommand? = null
            private set

        var createLedgerHandler: (CreateLedgerCommand) -> Ledger = { throw UnsupportedOperationException("stub") }
        var defineAccountHandler: (
            DefineAccountCommand,
        ) -> ChartOfAccounts = { throw UnsupportedOperationException("stub") }
        var postJournalEntryHandler: (
            PostJournalEntryCommand,
        ) -> JournalEntry = { throw UnsupportedOperationException("stub") }
        var closePeriodHandler: (
            CloseAccountingPeriodCommand,
        ) -> AccountingPeriod = { throw UnsupportedOperationException("stub") }
        var revaluationHandler: (
            RunCurrencyRevaluationCommand,
        ) -> JournalEntry? = { throw UnsupportedOperationException("stub") }

        override fun createLedger(command: CreateLedgerCommand): Ledger {
            lastCreateLedger = command
            return createLedgerHandler(command)
        }

        override fun defineAccount(command: DefineAccountCommand): ChartOfAccounts {
            lastDefineAccount = command
            return defineAccountHandler(command)
        }

        override fun postJournalEntry(command: PostJournalEntryCommand): JournalEntry {
            lastPostJournalEntry = command
            return postJournalEntryHandler(command)
        }

        override fun closePeriod(command: CloseAccountingPeriodCommand): AccountingPeriod {
            lastClosePeriod = command
            return closePeriodHandler(command)
        }

        override fun runCurrencyRevaluation(command: RunCurrencyRevaluationCommand): JournalEntry? {
            lastRevaluation = command
            return revaluationHandler(command)
        }
    }

    companion object {
        private val TENANT_ID: UUID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
    }
}
