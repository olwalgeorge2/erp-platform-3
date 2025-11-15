package com.erp.finance.accounting.domain.model

import java.time.Instant
import java.util.UUID

enum class DimensionRequirement {
    OPTIONAL,
    MANDATORY,
}

data class AccountDimensionPolicy(
    val id: UUID = UUID.randomUUID(),
    val tenantId: UUID,
    val accountType: AccountType,
    val dimensionType: DimensionType,
    val requirement: DimensionRequirement = DimensionRequirement.OPTIONAL,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
