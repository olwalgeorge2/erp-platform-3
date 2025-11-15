package com.erp.finance.accounting.infrastructure.adapter.input.rest.dto

import com.erp.finance.accounting.application.port.input.command.dimension.AssignFiscalYearVariantCommand
import com.erp.finance.accounting.application.port.input.command.dimension.CreateCompanyCodeCommand
import com.erp.finance.accounting.application.port.input.command.dimension.CreateFiscalYearVariantCommand
import com.erp.finance.accounting.application.port.input.command.dimension.DimensionQuery
import com.erp.finance.accounting.application.port.input.command.dimension.LinkLedgerToCompanyCodeCommand
import com.erp.finance.accounting.application.port.input.command.dimension.SchedulePeriodBlackoutCommand
import com.erp.finance.accounting.application.port.input.command.dimension.UpsertDimensionCommand
import com.erp.finance.accounting.application.port.input.command.dimension.UpsertDimensionPolicyCommand
import com.erp.finance.accounting.domain.model.AccountDimensionPolicy
import com.erp.finance.accounting.domain.model.AccountingDimension
import com.erp.finance.accounting.domain.model.CompanyCode
import com.erp.finance.accounting.domain.model.CompanyCodeFiscalYearVariant
import com.erp.finance.accounting.domain.model.DimensionRequirement
import com.erp.finance.accounting.domain.model.DimensionStatus
import com.erp.finance.accounting.domain.model.DimensionType
import com.erp.finance.accounting.domain.model.FiscalYearVariant
import com.erp.finance.accounting.domain.model.PeriodBlackout
import com.erp.financial.shared.validation.InputSanitizer.sanitizeAccountCode
import com.erp.financial.shared.validation.InputSanitizer.sanitizeForXss
import com.erp.financial.shared.validation.InputSanitizer.sanitizeText
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateCompanyCodeRequest(
    val tenantId: UUID,
    val code: String,
    val name: String,
    val legalEntityName: String,
    val countryCode: String,
    val baseCurrency: String,
    val timezone: String,
    val status: String = "ACTIVE",
) {
    fun toCommand(): CreateCompanyCodeCommand =
        CreateCompanyCodeCommand(
            tenantId = tenantId,
            code = code.sanitizeAccountCode(),
            name = name.sanitizeForXss(),
            legalEntityName = legalEntityName.sanitizeForXss(),
            countryCode = countryCode.trim().uppercase().take(2),
            baseCurrency = baseCurrency.trim().uppercase().take(3),
            timezone = timezone.trim(),
            status = status,
        )
}

data class CompanyCodeResponse(
    val id: UUID,
    val tenantId: UUID,
    val code: String,
    val name: String,
    val legalEntityName: String,
    val countryCode: String,
    val baseCurrency: String,
    val timezone: String,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun CompanyCode.toResponse(): CompanyCodeResponse =
    CompanyCodeResponse(
        id = id,
        tenantId = tenantId,
        code = code,
        name = name,
        legalEntityName = legalEntityName,
        countryCode = countryCode,
        baseCurrency = baseCurrency,
        timezone = timezone,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

data class DimensionRequest(
    val tenantId: UUID,
    val companyCodeId: UUID,
    val code: String,
    val name: String,
    val description: String? = null,
    val parentId: UUID? = null,
    val status: DimensionStatus = DimensionStatus.DRAFT,
    val validFrom: LocalDate,
    val validTo: LocalDate? = null,
) {
    fun toCommand(
        type: DimensionType,
        dimensionId: UUID? = null,
    ): UpsertDimensionCommand =
        UpsertDimensionCommand(
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            type = type,
            code = code.sanitizeAccountCode(),
            name = name.sanitizeForXss(),
            description = description?.sanitizeText(500),
            parentId = parentId,
            status = status,
            validFrom = validFrom,
            validTo = validTo,
            dimensionId = dimensionId,
        )
}

data class DimensionResponse(
    val id: UUID,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val type: DimensionType,
    val code: String,
    val name: String,
    val description: String?,
    val parentId: UUID?,
    val status: DimensionStatus,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun AccountingDimension.toResponse(): DimensionResponse =
    DimensionResponse(
        id = id,
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        type = type,
        code = code,
        name = name,
        description = description,
        parentId = parentId,
        status = status,
        validFrom = validFrom,
        validTo = validTo,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

data class FiscalYearVariantRequest(
    val tenantId: UUID,
    val code: String,
    val name: String,
    val description: String? = null,
    val startMonth: Int,
    val calendarPattern: String = "CALENDAR",
    val periods: List<FiscalYearVariantPeriodRequest>,
) {
    fun toCommand(): CreateFiscalYearVariantCommand =
        CreateFiscalYearVariantCommand(
            tenantId = tenantId,
            code = code.sanitizeAccountCode(),
            name = name.sanitizeForXss(),
            description = description?.sanitizeText(500),
            startMonth = startMonth,
            calendarPattern = calendarPattern,
            periods = periods.map(FiscalYearVariantPeriodRequest::toDefinition),
        )
}

data class FiscalYearVariantPeriodRequest(
    val periodNumber: Int,
    val label: String,
    val lengthInDays: Int,
    val adjustment: Boolean = false,
) {
    fun toDefinition(): com.erp.finance.accounting.application.port.input.command.dimension.VariantPeriodDefinition =
        com.erp.finance.accounting.application.port.input.command.dimension.VariantPeriodDefinition(
            periodNumber = periodNumber,
            label = label,
            lengthInDays = lengthInDays,
            adjustment = adjustment,
        )
}

data class FiscalYearVariantResponse(
    val id: UUID,
    val tenantId: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val startMonth: Int,
    val calendarPattern: String,
    val periods: List<FiscalYearVariantPeriodResponse>,
)

data class FiscalYearVariantPeriodResponse(
    val periodNumber: Int,
    val label: String,
    val lengthInDays: Int,
    val adjustment: Boolean,
)

fun FiscalYearVariant.toResponse(): FiscalYearVariantResponse =
    FiscalYearVariantResponse(
        id = id,
        tenantId = tenantId,
        code = code,
        name = name,
        description = description,
        startMonth = startMonth,
        calendarPattern = calendarPattern,
        periods =
            periods.map {
                FiscalYearVariantPeriodResponse(
                    periodNumber = it.periodNumber,
                    label = it.label,
                    lengthInDays = it.lengthInDays,
                    adjustment = it.adjustment,
                )
            },
    )

data class AssignFiscalYearVariantRequest(
    val tenantId: UUID,
    val fiscalYearVariantId: UUID,
    val effectiveFrom: Instant,
    val effectiveTo: Instant? = null,
) {
    fun toCommand(companyCodeId: UUID): AssignFiscalYearVariantCommand =
        AssignFiscalYearVariantCommand(
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            fiscalYearVariantId = fiscalYearVariantId,
            effectiveFrom = effectiveFrom,
            effectiveTo = effectiveTo,
        )
}

data class FiscalYearVariantAssignmentResponse(
    val companyCodeId: UUID,
    val fiscalYearVariantId: UUID,
    val effectiveFrom: Instant,
    val effectiveTo: Instant?,
)

fun CompanyCodeFiscalYearVariant.toResponse(): FiscalYearVariantAssignmentResponse =
    FiscalYearVariantAssignmentResponse(
        companyCodeId = companyCodeId,
        fiscalYearVariantId = fiscalYearVariantId,
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
    )

data class SchedulePeriodBlackoutRequest(
    val tenantId: UUID,
    val periodCode: String,
    val blackoutStart: Instant,
    val blackoutEnd: Instant,
    val status: String = "PLANNED",
    val reason: String? = null,
) {
    fun toCommand(companyCodeId: UUID): SchedulePeriodBlackoutCommand =
        SchedulePeriodBlackoutCommand(
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            periodCode = periodCode,
            blackoutStart = blackoutStart,
            blackoutEnd = blackoutEnd,
            status = status,
            reason = reason,
        )
}

data class PeriodBlackoutResponse(
    val id: UUID,
    val companyCodeId: UUID,
    val periodCode: String,
    val blackoutStart: Instant,
    val blackoutEnd: Instant,
    val status: String,
    val reason: String?,
)

fun PeriodBlackout.toResponse(): PeriodBlackoutResponse =
    PeriodBlackoutResponse(
        id = id,
        companyCodeId = companyCodeId,
        periodCode = periodCode,
        blackoutStart = blackoutStart,
        blackoutEnd = blackoutEnd,
        status = status,
        reason = reason,
    )

data class DimensionPolicyRequest(
    val tenantId: UUID,
    val accountType: String,
    val dimensionType: DimensionType,
    val requirement: DimensionRequirement,
) {
    fun toCommand(): UpsertDimensionPolicyCommand =
        UpsertDimensionPolicyCommand(
            tenantId = tenantId,
            accountType =
                com.erp.finance.accounting.domain.model.AccountType
                    .valueOf(accountType),
            dimensionType = dimensionType,
            requirement = requirement,
        )
}

data class DimensionPolicyResponse(
    val id: UUID,
    val tenantId: UUID,
    val accountType: String,
    val dimensionType: DimensionType,
    val requirement: DimensionRequirement,
)

fun AccountDimensionPolicy.toResponse(): DimensionPolicyResponse =
    DimensionPolicyResponse(
        id = id,
        tenantId = tenantId,
        accountType = accountType.name,
        dimensionType = dimensionType,
        requirement = requirement,
    )

data class DimensionListRequest(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val status: DimensionStatus? = null,
) {
    fun toQuery(type: DimensionType): DimensionQuery =
        DimensionQuery(
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            type = type,
            status = status,
        )
}

data class LinkLedgerRequest(
    val tenantId: UUID,
    val ledgerId: UUID,
) {
    fun toCommand(companyCodeId: UUID): LinkLedgerToCompanyCodeCommand =
        LinkLedgerToCompanyCodeCommand(
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            ledgerId = ledgerId,
        )
}
