package com.erp.finance.accounting.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

enum class PostingBatchStatus { OPEN, SUBMITTED, POSTED }

@Entity
@Table(name = "posting_batches", schema = "financial_accounting")
class PostingBatchEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "ledger_id", nullable = false)
    var ledgerId: UUID,
    @Column(name = "reference", length = 128)
    var reference: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: PostingBatchStatus = PostingBatchStatus.OPEN,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "submitted_at")
    var submittedAt: Instant? = null,
    @Column(name = "posted_at")
    var postedAt: Instant? = null,
    @Version
    @Column(name = "version")
    var version: Int? = 0,
)
