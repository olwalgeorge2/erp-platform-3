package com.erp.finance.accounting.infrastructure.adapter.output.event

import com.erp.finance.accounting.domain.model.AccountId
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.AccountingPeriodId
import com.erp.finance.accounting.domain.model.AccountingPeriodStatus
import com.erp.finance.accounting.domain.model.ChartOfAccountsId
import com.erp.finance.accounting.domain.model.EntryDirection
import com.erp.finance.accounting.domain.model.JournalEntry
import com.erp.finance.accounting.domain.model.JournalEntryLine
import com.erp.finance.accounting.domain.model.Ledger
import com.erp.finance.accounting.domain.model.LedgerId
import com.erp.finance.accounting.domain.model.Money
import com.erp.finance.accounting.infrastructure.outbox.FinanceOutboxEventEntity
import com.erp.finance.accounting.infrastructure.outbox.JournalPostedEventPayload
import com.erp.finance.accounting.infrastructure.outbox.PeriodStatusEventPayload
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class KafkaFinanceEventPublisherTest {
    private lateinit var journalEmitter: TestEmitter
    private lateinit var periodEmitter: TestEmitter
    private lateinit var publisher: KafkaFinanceEventPublisher

    @BeforeEach
    fun setup() {
        journalEmitter = TestEmitter()
        periodEmitter = TestEmitter()
        publisher = KafkaFinanceEventPublisher(journalEmitter, periodEmitter)
    }

    @Test
    fun `publishJournalPosted emits schema-aligned payload`() {
        val tenantId = UUID.randomUUID()
        val ledgerId = LedgerId()
        val periodId = AccountingPeriodId()
        val entry =
            JournalEntry
                .draft(
                    tenantId = tenantId,
                    ledgerId = ledgerId,
                    periodId = periodId,
                    lines =
                        listOf(
                            JournalEntryLine(
                                accountId = AccountId(),
                                direction = EntryDirection.DEBIT,
                                amount = Money(5_000),
                                currency = "USD",
                                description = "Cash",
                            ),
                            JournalEntryLine(
                                accountId = AccountId(),
                                direction = EntryDirection.CREDIT,
                                amount = Money(5_000),
                                currency = "USD",
                                description = "Revenue",
                            ),
                        ),
                    reference = "JE-1",
                    description = "Sample posting",
                    bookedAt = Instant.parse("2025-01-02T00:00:00Z"),
                ).post(Instant.parse("2025-01-02T01:00:00Z"))

        val payload = mapper().writeValueAsString(JournalPostedEventPayload.from(entry))
        val event =
            FinanceOutboxEventEntity(
                eventType = "finance.journal.posted",
                channel = "finance-journal-events-out",
                payload = payload,
                occurredAt = entry.postedAt ?: entry.bookedAt,
            )

        publisher.publish(event)

        val parsed = mapper().readTree(journalEmitter.payloads.single())
        assertEquals("finance.journal.posted", parsed["eventType"].asText())
        assertEquals(entry.id.toString(), parsed["journalEntryId"].asText())
        assertEquals(tenantId.toString(), parsed["tenantId"].asText())
        assertEquals(5_000, parsed["totalDebitsMinor"].asLong())
        assertEquals(5_000, parsed["totalCreditsMinor"].asLong())
        assertEquals("USD", parsed["currency"].asText())
        assertEquals(2, parsed["lines"].size())
    }

    @Test
    fun `publishPeriodUpdated emits state transition`() {
        val ledger =
            Ledger(
                id = LedgerId(),
                tenantId = UUID.randomUUID(),
                chartOfAccountsId = ChartOfAccountsId(),
                baseCurrency = "USD",
            )
        val period =
            AccountingPeriod(
                ledgerId = ledger.id,
                tenantId = ledger.tenantId,
                code = "2025-01",
                startDate = LocalDate.parse("2025-01-01"),
                endDate = LocalDate.parse("2025-01-31"),
                status = AccountingPeriodStatus.CLOSED,
            )

        val payload =
            mapper().writeValueAsString(
                PeriodStatusEventPayload.from(period, AccountingPeriodStatus.FROZEN),
            )
        val event =
            FinanceOutboxEventEntity(
                eventType = "finance.period.closed",
                channel = "finance-period-events-out",
                payload = payload,
                occurredAt = period.startDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            )

        publisher.publish(event)

        val parsed = mapper().readTree(periodEmitter.payloads.single())
        assertEquals("finance.period.closed", parsed["eventType"].asText())
        assertEquals(period.id.value.toString(), parsed["periodId"].asText())
        assertEquals("FROZEN", parsed["previousStatus"].asText())
        assertEquals("CLOSED", parsed["currentStatus"].asText())
        assertEquals(false, parsed["freezeOnly"].asBoolean())
    }

    private fun mapper(): ObjectMapper =
        ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())

    private class TestEmitter : Emitter<String> {
        val payloads: MutableList<String> = mutableListOf()

        override fun send(payload: String): CompletionStage<Void> {
            payloads += payload
            return CompletableFuture.completedFuture(null)
        }

        override fun <M : Message<out String>> send(msg: M) {
            payloads += msg.payload
        }

        override fun complete() {}

        override fun error(exception: Exception?): Unit = throw exception ?: RuntimeException("Emitter error")

        override fun isCancelled(): Boolean = false

        override fun hasRequests(): Boolean = true
    }
}
