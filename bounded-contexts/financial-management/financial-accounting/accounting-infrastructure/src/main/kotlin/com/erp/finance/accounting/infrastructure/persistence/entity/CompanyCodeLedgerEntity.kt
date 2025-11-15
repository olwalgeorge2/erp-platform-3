package com.erp.finance.accounting.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "company_code_ledgers", schema = "financial_accounting")
class CompanyCodeLedgerEntity(
    @EmbeddedId
    var id: CompanyCodeLedgerKey = CompanyCodeLedgerKey(),
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)

data class CompanyCodeLedgerKey(
    @Column(name = "company_code_id", nullable = false)
    var companyCodeId: UUID = UUID.randomUUID(),
    @Column(name = "ledger_id", nullable = false)
    var ledgerId: UUID = UUID.randomUUID(),
) : java.io.Serializable
