package com.erp.finance.accounting.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class JournalEntryTest {
    private val tenantId = UUID.randomUUID()
    private val ledgerId = LedgerId()
    private val periodId = AccountingPeriodId()

    @Test
    fun `creates balanced draft and posts`() {
        val debitLine =
            JournalEntryLine(
                accountId = AccountId(),
                direction = EntryDirection.DEBIT,
                amount = Money(10_000),
                currency = "USD",
                description = "Cash",
            )
        val creditLine =
            JournalEntryLine(
                accountId = AccountId(),
                direction = EntryDirection.CREDIT,
                amount = Money(10_000),
                currency = "USD",
                description = "Revenue",
            )

        val draft =
            JournalEntry.draft(
                tenantId = tenantId,
                ledgerId = ledgerId,
                periodId = periodId,
                lines = listOf(debitLine, creditLine),
                reference = "JE-1",
                bookedAt = Instant.parse("2025-01-02T00:00:00Z"),
            )

        val posted = draft.post()

        assertEquals(JournalEntryStatus.DRAFT, draft.status)
        assertEquals(JournalEntryStatus.POSTED, posted.status)
        assertEquals("JE-1", posted.reference)
    }

    @Test
    fun `rejects unbalanced lines`() {
        val debitLine =
            JournalEntryLine(
                accountId = AccountId(),
                direction = EntryDirection.DEBIT,
                amount = Money(5_000),
                currency = "USD",
            )
        val creditLine =
            JournalEntryLine(
                accountId = AccountId(),
                direction = EntryDirection.CREDIT,
                amount = Money(7_000),
                currency = "USD",
            )

        assertThrows(IllegalArgumentException::class.java) {
            JournalEntry.draft(
                tenantId = tenantId,
                ledgerId = ledgerId,
                periodId = periodId,
                lines = listOf(debitLine, creditLine),
            )
        }
    }
}
