package com.erp.finance.accounting.application.port.input.command.dimension

import com.erp.finance.accounting.domain.model.DimensionRequirement
import com.erp.finance.accounting.domain.model.DimensionStatus
import com.erp.finance.accounting.domain.model.DimensionType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateCompanyCodeCommand(
    val tenantId: UUID,
    val code: String,
    val name: String,
    val legalEntityName: String,
    val countryCode: String,
    val baseCurrency: String,
    val timezone: String,
    val status: String = "ACTIVE",
)

data class UpsertDimensionCommand(
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
    val dimensionId: UUID? = null,
)

data class DimensionQuery(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val type: DimensionType,
    val status: DimensionStatus? = null,
)

data class CreateFiscalYearVariantCommand(
    val tenantId: UUID,
    val code: String,
    val name: String,
    val description: String? = null,
    val startMonth: Int,
    val calendarPattern: String = "CALENDAR",
    val periods: List<VariantPeriodDefinition>,
)

data class VariantPeriodDefinition(
    val periodNumber: Int,
    val label: String,
    val lengthInDays: Int,
    val adjustment: Boolean = false,
)

data class AssignFiscalYearVariantCommand(
    val tenantId: UUID,
    val companyCodeId: UUID,
    val fiscalYearVariantId: UUID,
    val effectiveFrom: Instant,
    val effectiveTo: Instant? = null,
)

data class SchedulePeriodBlackoutCommand(
    val tenantId: UUID,
    val companyCodeId: UUID,
    val periodCode: String,
    val blackoutStart: Instant,
    val blackoutEnd: Instant,
    val status: String = "PLANNED",
    val reason: String? = null,
)

data class UpsertDimensionPolicyCommand(
    val tenantId: UUID,
    val accountType: com.erp.finance.accounting.domain.model.AccountType,
    val dimensionType: DimensionType,
    val requirement: DimensionRequirement,
)

data class LinkLedgerToCompanyCodeCommand(
    val tenantId: UUID,
    val companyCodeId: UUID,
    val ledgerId: UUID,
)
