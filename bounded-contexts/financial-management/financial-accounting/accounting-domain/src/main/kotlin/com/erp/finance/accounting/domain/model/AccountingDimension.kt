package com.erp.finance.accounting.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class AccountingDimension(
    val id: UUID = UUID.randomUUID(),
    val tenantId: UUID,
    val companyCodeId: UUID,
    val type: DimensionType,
    val code: String,
    val name: String,
    val description: String? = null,
    val parentId: UUID? = null,
    val status: DimensionStatus = DimensionStatus.DRAFT,
    val validFrom: LocalDate,
    val validTo: LocalDate? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    init {
        require(code.isNotBlank()) { "Dimension code cannot be blank" }
        require(name.isNotBlank()) { "Dimension name cannot be blank" }
        require(validTo == null || !validTo.isBefore(validFrom)) { "validTo cannot be before validFrom" }
    }

    fun activate(): AccountingDimension = copy(status = DimensionStatus.ACTIVE, updatedAt = Instant.now())

    fun retire(): AccountingDimension = copy(status = DimensionStatus.RETIRED, updatedAt = Instant.now())

    fun isActive(on: LocalDate): Boolean =
        status == DimensionStatus.ACTIVE &&
            !validFrom.isAfter(on) &&
            (validTo == null || !validTo.isBefore(on))
}
