package com.erp.finance.accounting.domain.model

import java.time.Instant
import java.util.UUID

enum class ControlAccountSubLedger { AP, AR }

enum class ControlAccountCategory { PAYABLE, RECEIVABLE }

data class ControlAccountConfig(
    val id: UUID = UUID.randomUUID(),
    val tenantId: UUID,
    val companyCodeId: UUID,
    val subLedger: ControlAccountSubLedger,
    val category: ControlAccountCategory,
    val dimensionKey: String,
    val currency: String,
    val glAccountId: UUID,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
