package com.erp.finance.accounting.domain.model

import java.time.Instant
import java.util.UUID

enum class JournalEntryStatus { DRAFT, POSTED }

enum class EntryDirection { DEBIT, CREDIT }

data class JournalEntryLine(
    val id: UUID = UUID.randomUUID(),
    val accountId: AccountId,
    val direction: EntryDirection,
    val amount: Money,
    val currency: String,
    val description: String? = null,
    val createdAt: Instant = Instant.now(),
    val originalCurrency: String = currency,
    val originalAmount: Money = amount,
    val dimensions: DimensionAssignments = DimensionAssignments(),
) {
    init {
        require(amount.amount > 0) { "Line amount must be positive" }
        require(currency.length == 3) { "Currency must be ISO-4217 3-letter code" }
        require(originalCurrency.length == 3) { "Original currency must be ISO-4217 3-letter code" }
        require(originalAmount.amount > 0) { "Original amount must be positive" }
    }
}

class JournalEntry private constructor(
    val id: UUID,
    val tenantId: UUID,
    val ledgerId: LedgerId,
    val accountingPeriodId: AccountingPeriodId,
    val lines: List<JournalEntryLine>,
    val reference: String?,
    val description: String?,
    val bookedAt: Instant,
    val status: JournalEntryStatus,
    val postedAt: Instant?,
) {
    companion object {
        fun draft(
            tenantId: UUID,
            ledgerId: LedgerId,
            periodId: AccountingPeriodId,
            lines: List<JournalEntryLine>,
            reference: String? = null,
            description: String? = null,
            bookedAt: Instant = Instant.now(),
        ): JournalEntry {
            require(lines.size >= 2) { "Journal entry requires at least 2 lines" }
            require(hasBalancedLines(lines)) { "Debits and credits must balance" }
            require(lines.map(JournalEntryLine::currency).distinct().size == 1) {
                "All journal entry lines must use the same currency"
            }

            return JournalEntry(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                ledgerId = ledgerId,
                accountingPeriodId = periodId,
                lines = lines.sortedBy { it.direction.name },
                reference = reference,
                description = description,
                bookedAt = bookedAt,
                status = JournalEntryStatus.DRAFT,
                postedAt = null,
            )
        }

        fun fromPersistence(
            id: UUID,
            tenantId: UUID,
            ledgerId: LedgerId,
            periodId: AccountingPeriodId,
            lines: List<JournalEntryLine>,
            reference: String?,
            description: String?,
            bookedAt: Instant,
            status: JournalEntryStatus,
            postedAt: Instant?,
        ): JournalEntry =
            JournalEntry(
                id = id,
                tenantId = tenantId,
                ledgerId = ledgerId,
                accountingPeriodId = periodId,
                lines = lines.sortedBy { it.direction.name },
                reference = reference,
                description = description,
                bookedAt = bookedAt,
                status = status,
                postedAt = postedAt,
            )

        private fun hasBalancedLines(lines: List<JournalEntryLine>): Boolean {
            val totalDebits =
                lines
                    .filter { it.direction == EntryDirection.DEBIT }
                    .fold(Money.ZERO) { acc, line -> acc + line.amount }
            val totalCredits =
                lines
                    .filter { it.direction == EntryDirection.CREDIT }
                    .fold(Money.ZERO) { acc, line -> acc + line.amount }
            return totalDebits.amount == totalCredits.amount
        }
    }

    fun post(at: Instant = Instant.now()): JournalEntry {
        require(status == JournalEntryStatus.DRAFT) { "Only draft entries can be posted" }
        return copy(status = JournalEntryStatus.POSTED, postedAt = at)
    }

    fun ensurePosted(): JournalEntry = if (status == JournalEntryStatus.POSTED) this else error("Entry must be posted")

    private fun copy(
        status: JournalEntryStatus = this.status,
        postedAt: Instant? = this.postedAt,
    ): JournalEntry =
        JournalEntry(
            id = id,
            tenantId = tenantId,
            ledgerId = ledgerId,
            accountingPeriodId = accountingPeriodId,
            lines = lines,
            reference = reference,
            description = description,
            bookedAt = bookedAt,
            status = status,
            postedAt = postedAt,
        )
}
