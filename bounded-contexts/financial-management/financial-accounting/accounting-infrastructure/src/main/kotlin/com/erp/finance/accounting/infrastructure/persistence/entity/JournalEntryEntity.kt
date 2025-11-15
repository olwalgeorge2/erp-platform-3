package com.erp.finance.accounting.infrastructure.persistence.entity

import com.erp.finance.accounting.domain.model.AccountId
import com.erp.finance.accounting.domain.model.AccountingPeriodId
import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.finance.accounting.domain.model.EntryDirection
import com.erp.finance.accounting.domain.model.JournalEntry
import com.erp.finance.accounting.domain.model.JournalEntryLine
import com.erp.finance.accounting.domain.model.JournalEntryStatus
import com.erp.finance.accounting.domain.model.LedgerId
import com.erp.finance.accounting.domain.model.Money
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "journal_entries", schema = "financial_accounting")
class JournalEntryEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "ledger_id", nullable = false)
    var ledgerId: UUID,
    @Column(name = "accounting_period_id", nullable = false)
    var accountingPeriodId: UUID,
    @Column(name = "posting_batch_id")
    var postingBatchId: UUID? = null,
    @Column(name = "reference", length = 128)
    var reference: String? = null,
    @Lob
    @Column(name = "description")
    var description: String? = null,
    @Column(name = "booked_at", nullable = false)
    var bookedAt: Instant = Instant.now(),
    @Column(name = "posted_at")
    var postedAt: Instant? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: JournalEntryStatus = JournalEntryStatus.DRAFT,
    @Column(name = "total_debits", nullable = false)
    var totalDebits: Long = 0,
    @Column(name = "total_credits", nullable = false)
    var totalCredits: Long = 0,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Column(name = "created_by", nullable = false, length = 128)
    var createdBy: String = DEFAULT_ACTOR,
    @Column(name = "updated_by", nullable = false, length = 128)
    var updatedBy: String = DEFAULT_ACTOR,
    @Column(name = "source_system", nullable = false, length = 64)
    var sourceSystem: String = DEFAULT_SOURCE,
    @Version
    @Column(name = "version")
    var version: Int? = 0,
) {
    @PrePersist
    fun prePersist() {
        createdBy = createdBy.ifBlank { DEFAULT_ACTOR }
        updatedBy = updatedBy.ifBlank { createdBy }
        sourceSystem = sourceSystem.ifBlank { DEFAULT_SOURCE }
        updatedAt = Instant.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedBy = updatedBy.ifBlank { DEFAULT_ACTOR }
        sourceSystem = sourceSystem.ifBlank { DEFAULT_SOURCE }
        updatedAt = Instant.now()
    }

    @OneToMany(
        mappedBy = "journalEntry",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    var lines: MutableList<JournalEntryLineEntity> = mutableListOf()

    fun toDomain(): JournalEntry =
        JournalEntry.fromPersistence(
            id = id,
            tenantId = tenantId,
            ledgerId = LedgerId(ledgerId),
            periodId = AccountingPeriodId(accountingPeriodId),
            lines =
                lines
                    .sortedBy(JournalEntryLineEntity::lineNumber)
                    .map(JournalEntryLineEntity::toDomain),
            reference = reference,
            description = description,
            bookedAt = bookedAt,
            status = status,
            postedAt = postedAt,
        )

    fun updateFrom(domain: JournalEntry) {
        tenantId = domain.tenantId
        ledgerId = domain.ledgerId.value
        accountingPeriodId = domain.accountingPeriodId.value
        reference = domain.reference
        description = domain.description
        bookedAt = domain.bookedAt
        status = domain.status
        postedAt = domain.postedAt
        updatedAt = Instant.now()

        val totals =
            domain.lines.fold(0L to 0L) { acc, line ->
                val (debits, credits) = acc
                if (line.direction == EntryDirection.DEBIT) {
                    (debits + line.amount.amount) to credits
                } else {
                    debits to (credits + line.amount.amount)
                }
            }
        totalDebits = totals.first
        totalCredits = totals.second

        lines.clear()
        domain.lines.forEachIndexed { index, line ->
            lines +=
                JournalEntryLineEntity(
                    id = line.id,
                    journalEntry = this,
                    accountId = line.accountId.value,
                    tenantId = domain.tenantId,
                    lineNumber = index + 1,
                    debitAmount = if (line.direction == EntryDirection.DEBIT) line.amount.amount else 0,
                    creditAmount = if (line.direction == EntryDirection.CREDIT) line.amount.amount else 0,
                    currency = line.currency,
                    description = line.description,
                    createdAt = line.createdAt,
                    originalCurrency = line.originalCurrency,
                    originalAmount = line.originalAmount.amount,
                    costCenterId = line.dimensions.costCenterId,
                    profitCenterId = line.dimensions.profitCenterId,
                    departmentId = line.dimensions.departmentId,
                    projectId = line.dimensions.projectId,
                    businessAreaId = line.dimensions.businessAreaId,
                )
        }
    }

    companion object {
        fun from(domain: JournalEntry): JournalEntryEntity =
            JournalEntryEntity(
                id = domain.id,
                tenantId = domain.tenantId,
                ledgerId = domain.ledgerId.value,
                accountingPeriodId = domain.accountingPeriodId.value,
                reference = domain.reference,
                description = domain.description,
                bookedAt = domain.bookedAt,
                status = domain.status,
                postedAt = domain.postedAt,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ).also { entity ->
                entity.updateFrom(domain)
            }

        private const val DEFAULT_ACTOR = "system"
        private const val DEFAULT_SOURCE = "erp-platform"
    }
}

@Entity
@Table(name = "journal_entry_lines", schema = "financial_accounting")
class JournalEntryLineEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    var journalEntry: JournalEntryEntity,
    @Column(name = "account_id", nullable = false)
    var accountId: UUID,
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "line_number", nullable = false)
    var lineNumber: Int,
    @Column(name = "debit_amount", nullable = false)
    var debitAmount: Long = 0,
    @Column(name = "credit_amount", nullable = false)
    var creditAmount: Long = 0,
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,
    @Lob
    @Column(name = "description")
    var description: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "original_currency", nullable = false, length = 3)
    var originalCurrency: String = currency,
    @Column(name = "original_amount", nullable = false)
    var originalAmount: Long = 0,
    @Column(name = "cost_center_id")
    var costCenterId: UUID? = null,
    @Column(name = "profit_center_id")
    var profitCenterId: UUID? = null,
    @Column(name = "department_id")
    var departmentId: UUID? = null,
    @Column(name = "project_id")
    var projectId: UUID? = null,
    @Column(name = "business_area_id")
    var businessAreaId: UUID? = null,
) {
    fun toDomain(): JournalEntryLine =
        JournalEntryLine(
            id = id,
            accountId = AccountId(accountId),
            direction = if (debitAmount > 0) EntryDirection.DEBIT else EntryDirection.CREDIT,
            amount = Money(if (debitAmount > 0) debitAmount else creditAmount),
            currency = currency,
            description = description,
            createdAt = createdAt,
            originalCurrency = originalCurrency,
            originalAmount = Money(originalAmount),
            dimensions =
                DimensionAssignments(
                    costCenterId = costCenterId,
                    profitCenterId = profitCenterId,
                    departmentId = departmentId,
                    projectId = projectId,
                    businessAreaId = businessAreaId,
                ),
        )
}
