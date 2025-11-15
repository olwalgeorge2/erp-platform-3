package com.erp.finance.accounting.domain.model

import java.time.Instant
import java.util.UUID

data class FiscalYearVariant(
    val id: UUID = UUID.randomUUID(),
    val tenantId: UUID,
    val code: String,
    val name: String,
    val description: String? = null,
    val startMonth: Int,
    val calendarPattern: String = "CALENDAR",
    val periods: List<FiscalYearVariantPeriod> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    init {
        require(code.isNotBlank()) { "Fiscal year variant code cannot be blank" }
        require(name.isNotBlank()) { "Fiscal year variant name cannot be blank" }
        require(startMonth in 1..12) { "startMonth must be between 1 and 12" }
    }
}

data class FiscalYearVariantPeriod(
    val periodNumber: Int,
    val label: String,
    val lengthInDays: Int,
    val adjustment: Boolean = false,
) {
    init {
        require(periodNumber > 0) { "periodNumber must be positive" }
        require(label.isNotBlank()) { "period label cannot be blank" }
        require(lengthInDays > 0) { "lengthInDays must be positive" }
    }
}

data class CompanyCodeFiscalYearVariant(
    val companyCodeId: UUID,
    val fiscalYearVariantId: UUID,
    val effectiveFrom: Instant,
    val effectiveTo: Instant? = null,
)

data class PeriodBlackout(
    val id: UUID = UUID.randomUUID(),
    val companyCodeId: UUID,
    val periodCode: String,
    val blackoutStart: Instant,
    val blackoutEnd: Instant,
    val status: String = "PLANNED",
    val reason: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    init {
        require(periodCode.isNotBlank()) { "Period code cannot be blank" }
        require(!blackoutEnd.isBefore(blackoutStart)) { "blackoutEnd cannot be before blackoutStart" }
    }
}
