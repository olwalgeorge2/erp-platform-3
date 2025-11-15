package com.erp.finance.accounting.infrastructure.adapter.input.rest.dto

import com.erp.finance.accounting.application.port.input.command.CloseAccountingPeriodCommand
import com.erp.finance.accounting.application.port.input.command.CreateLedgerCommand
import com.erp.finance.accounting.application.port.input.command.DefineAccountCommand
import com.erp.finance.accounting.application.port.input.command.JournalEntryLineCommand
import com.erp.finance.accounting.application.port.input.command.PostJournalEntryCommand
import com.erp.finance.accounting.application.port.input.dto.LedgerPeriodInfoDto
import com.erp.finance.accounting.domain.model.Account
import com.erp.finance.accounting.domain.model.AccountId
import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.ChartOfAccounts
import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.finance.accounting.domain.model.EntryDirection
import com.erp.finance.accounting.domain.model.JournalEntry
import com.erp.finance.accounting.domain.model.JournalEntryLine
import com.erp.finance.accounting.domain.model.Ledger
import com.erp.finance.accounting.domain.model.Money
import com.erp.financial.shared.validation.FinanceValidationErrorCode
import com.erp.financial.shared.validation.FinanceValidationException
import com.erp.financial.shared.validation.InputSanitizer.sanitizeAccountCode
import com.erp.financial.shared.validation.InputSanitizer.sanitizeForXss
import com.erp.financial.shared.validation.InputSanitizer.sanitizeReferenceNumber
import com.erp.financial.shared.validation.InputSanitizer.sanitizeText
import com.erp.financial.shared.validation.ValidationMessageResolver
import com.erp.financial.shared.validation.constraints.ValidAccountCode
import com.erp.financial.shared.validation.constraints.ValidCurrencyCode
import com.erp.financial.shared.validation.constraints.ValidDateRange
import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import com.erp.finance.accounting.application.port.input.dto.AccountingPeriodDto as QueryAccountingPeriodDto

@RegisterForReflection
data class CreateLedgerRequest(
    @field:NotNull
    val tenantId: UUID,
    @field:NotNull
    val chartOfAccountsId: UUID,
    @field:NotBlank
    @field:ValidCurrencyCode
    val baseCurrency: String,
    @field:ValidAccountCode
    val chartCode: String? = null,
    val chartName: String? = null,
    val reference: String? = null,
) {
    fun toCommand(locale: Locale): CreateLedgerCommand {
        val normalizedCurrency = normalizeCurrency(baseCurrency, "baseCurrency", locale)
        val normalizedChartCode = chartCode?.takeIf { it.isNotBlank() }?.sanitizeAccountCode() ?: "DEFAULT"
        val normalizedChartName = chartName?.takeIf { it.isNotBlank() }?.sanitizeForXss() ?: "Default Chart"
        return CreateLedgerCommand(
            tenantId = tenantId,
            chartOfAccountsId = chartOfAccountsId,
            baseCurrency = normalizedCurrency,
            chartCode = normalizedChartCode,
            chartName = normalizedChartName,
            reference = reference?.sanitizeReferenceNumber(),
        )
    }
}

@RegisterForReflection
data class DefineAccountRequest(
    @field:NotNull
    val tenantId: UUID,
    val parentAccountId: UUID? = null,
    @field:NotBlank
    @field:ValidAccountCode
    val code: String,
    @field:NotBlank
    val name: String,
    @field:NotNull
    val type: AccountType,
    @field:ValidCurrencyCode
    val currency: String? = null,
    val isPosting: Boolean = true,
) {
    fun toCommand(
        locale: Locale,
        chartOfAccountsId: UUID,
    ): DefineAccountCommand =
        DefineAccountCommand(
            tenantId = tenantId,
            chartOfAccountsId = chartOfAccountsId,
            parentAccountId = parentAccountId,
            code = code.sanitizeAccountCode(),
            name = name.sanitizeForXss(),
            type = type,
            currency = currency?.takeIf { it.isNotBlank() }?.let { normalizeCurrency(it, "currency", locale) },
            isPosting = isPosting,
        )
}

@RegisterForReflection
data class PostJournalEntryRequest(
    @field:NotNull
    val tenantId: UUID,
    @field:NotNull
    val ledgerId: UUID,
    @field:NotNull
    val accountingPeriodId: UUID,
    val reference: String? = null,
    val description: String? = null,
    val bookedAt: Instant? = null,
    @field:NotEmpty
    @field:Valid
    val lines: List<PostJournalEntryLineRequest>,
) {
    fun toCommand(locale: Locale): PostJournalEntryCommand {
        val resolvedLines =
            lines.takeIf { it.size >= 2 }
                ?: throw FinanceValidationException(
                    errorCode = FinanceValidationErrorCode.FINANCE_INVALID_JOURNAL_LINES,
                    field = "lines",
                    rejectedValue = null,
                    locale = locale,
                    message =
                        ValidationMessageResolver.resolve(
                            FinanceValidationErrorCode.FINANCE_INVALID_JOURNAL_LINES,
                            locale,
                        ),
                )

        return PostJournalEntryCommand(
            tenantId = tenantId,
            ledgerId = ledgerId,
            accountingPeriodId = accountingPeriodId,
            reference = reference?.sanitizeReferenceNumber(),
            description = description?.sanitizeText(500),
            bookedAt = bookedAt ?: Instant.now(),
            lines = resolvedLines.mapIndexed { index, line -> line.toCommand(index, locale) },
        )
    }
}

@RegisterForReflection
data class PostJournalEntryLineRequest(
    @field:NotNull
    val accountId: UUID,
    @field:NotNull
    val direction: EntryDirection,
    @Schema(description = "Monetary amount expressed in minor units (e.g. cents)")
    val amountMinor: Long,
    @field:ValidCurrencyCode
    val currency: String? = null,
    val description: String? = null,
    val costCenterId: UUID? = null,
    val profitCenterId: UUID? = null,
    val departmentId: UUID? = null,
    val projectId: UUID? = null,
    val businessAreaId: UUID? = null,
) {
    fun toCommand(
        index: Int,
        locale: Locale,
    ): JournalEntryLineCommand {
        if (amountMinor <= 0) {
            val field = "lines[$index].amountMinor"
            throw FinanceValidationException(
                errorCode = FinanceValidationErrorCode.FINANCE_INVALID_JOURNAL_LINE_AMOUNT,
                field = field,
                rejectedValue = amountMinor.toString(),
                locale = locale,
                message =
                    ValidationMessageResolver.resolve(
                        FinanceValidationErrorCode.FINANCE_INVALID_JOURNAL_LINE_AMOUNT,
                        locale,
                        field,
                    ),
            )
        }

        return JournalEntryLineCommand(
            accountId = AccountId(accountId),
            direction = direction,
            amount = Money(amountMinor),
            currency =
                currency?.takeIf { it.isNotBlank() }?.let {
                    normalizeCurrency(
                        it,
                        "lines[$index].currency",
                        locale,
                    )
                },
            description = description?.sanitizeText(200),
            dimensions = toAssignments(),
        )
    }
}

@RegisterForReflection
data class ClosePeriodRequest(
    @field:NotNull
    val tenantId: UUID,
    val freezeOnly: Boolean = false,
) {
    fun toCommand(
        ledgerId: UUID,
        periodId: UUID,
    ): CloseAccountingPeriodCommand =
        CloseAccountingPeriodCommand(
            tenantId = tenantId,
            ledgerId = ledgerId,
            accountingPeriodId = periodId,
            freezeOnly = freezeOnly,
        )
}

@RegisterForReflection
data class RunCurrencyRevaluationRequest(
    @field:NotNull
    val tenantId: UUID,
    @field:NotNull
    val gainAccountId: UUID,
    @field:NotNull
    val lossAccountId: UUID,
    val asOfTimestamp: Instant? = null,
    val bookedAt: Instant? = null,
    val reference: String? = null,
    val description: String? = null,
) {
    fun toCommand(
        ledgerId: UUID,
        periodId: UUID,
    ): com.erp.finance.accounting.application.port.input.command.RunCurrencyRevaluationCommand {
        val asOf = asOfTimestamp ?: Instant.now()
        return com.erp.finance.accounting.application.port.input.command.RunCurrencyRevaluationCommand(
            tenantId = tenantId,
            ledgerId = ledgerId,
            accountingPeriodId = periodId,
            asOf = asOf,
            bookedAt = bookedAt ?: asOf,
            gainAccountId = AccountId(gainAccountId),
            lossAccountId = AccountId(lossAccountId),
            reference = reference,
            description = description,
        )
    }
}

data class LedgerResponse(
    val id: UUID,
    val tenantId: UUID,
    val chartOfAccountsId: UUID,
    val baseCurrency: String,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ChartOfAccountsResponse(
    val id: UUID,
    val tenantId: UUID,
    val code: String,
    val name: String,
    val baseCurrency: String,
    val accounts: List<AccountResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class AccountResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val type: AccountType,
    val currency: String,
    val parentAccountId: UUID?,
    val isPosting: Boolean,
)

data class JournalEntryResponse(
    val id: UUID,
    val tenantId: UUID,
    val ledgerId: UUID,
    val accountingPeriodId: UUID,
    val status: String,
    val reference: String?,
    val description: String?,
    val bookedAt: Instant,
    val postedAt: Instant?,
    val lines: List<JournalEntryLineResponse>,
)

data class JournalEntryLineResponse(
    val id: UUID,
    val accountId: UUID,
    val direction: EntryDirection,
    val amountMinor: Long,
    val currency: String,
    val description: String?,
    val dimensions: DimensionAssignmentResponse,
)

data class DimensionAssignmentResponse(
    val costCenterId: UUID?,
    val profitCenterId: UUID?,
    val departmentId: UUID?,
    val projectId: UUID?,
    val businessAreaId: UUID?,
)

data class AccountingPeriodResponse(
    val id: UUID,
    val ledgerId: UUID,
    val tenantId: UUID,
    val code: String,
    val status: String,
    val startDate: String,
    val endDate: String,
)

data class LedgerPeriodInfoResponse(
    val ledgerId: UUID,
    val ledgerCode: String,
    val currentOpenPeriod: LedgerAccountingPeriodResponse?,
    val openPeriods: List<LedgerAccountingPeriodResponse>,
)

@ValidDateRange(startField = "startDate", endField = "endDate")
data class LedgerAccountingPeriodResponse(
    val periodId: UUID,
    val code: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: String,
)

private fun normalizeCurrency(
    value: String,
    field: String,
    locale: Locale,
): String {
    val normalized = value.trim().uppercase(Locale.getDefault())
    if (normalized.length != 3) {
        throw FinanceValidationException(
            errorCode = FinanceValidationErrorCode.FINANCE_INVALID_CURRENCY_CODE,
            field = field,
            rejectedValue = value,
            locale = locale,
            message =
                ValidationMessageResolver.resolve(
                    FinanceValidationErrorCode.FINANCE_INVALID_CURRENCY_CODE,
                    locale,
                    value,
                ),
        )
    }
    return normalized
}

fun Ledger.toResponse(): LedgerResponse =
    LedgerResponse(
        id = id.value,
        tenantId = tenantId,
        chartOfAccountsId = chartOfAccountsId.value,
        baseCurrency = baseCurrency,
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ChartOfAccounts.toResponse(): ChartOfAccountsResponse =
    ChartOfAccountsResponse(
        id = id.value,
        tenantId = tenantId,
        code = code,
        name = name,
        baseCurrency = baseCurrency,
        accounts = accounts.values.map(Account::toResponse),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Account.toResponse(): AccountResponse =
    AccountResponse(
        id = id.value,
        code = code,
        name = name,
        type = type,
        currency = currency,
        parentAccountId = parentAccountId?.value,
        isPosting = isPosting,
    )

fun JournalEntry.toResponse(): JournalEntryResponse =
    JournalEntryResponse(
        id = id,
        tenantId = tenantId,
        ledgerId = ledgerId.value,
        accountingPeriodId = accountingPeriodId.value,
        status = status.name,
        reference = reference,
        description = description,
        bookedAt = bookedAt,
        postedAt = postedAt,
        lines = lines.map(JournalEntryLine::toResponse),
    )

fun JournalEntryLine.toResponse(): JournalEntryLineResponse =
    JournalEntryLineResponse(
        id = id,
        accountId = accountId.value,
        direction = direction,
        amountMinor = amount.amount,
        currency = currency,
        description = description,
        dimensions =
            DimensionAssignmentResponse(
                costCenterId = dimensions.costCenterId,
                profitCenterId = dimensions.profitCenterId,
                departmentId = dimensions.departmentId,
                projectId = dimensions.projectId,
                businessAreaId = dimensions.businessAreaId,
            ),
    )

fun AccountingPeriod.toResponse(): AccountingPeriodResponse =
    AccountingPeriodResponse(
        id = id.value,
        ledgerId = ledgerId.value,
        tenantId = tenantId,
        code = code,
        status = status.name,
        startDate = startDate.toString(),
        endDate = endDate.toString(),
    )

fun LedgerPeriodInfoDto.toResponse(): LedgerPeriodInfoResponse =
    LedgerPeriodInfoResponse(
        ledgerId = ledgerId,
        ledgerCode = ledgerCode,
        currentOpenPeriod = currentOpenPeriod?.toLedgerResponse(),
        openPeriods = openPeriods.map { it.toLedgerResponse() },
    )

private fun QueryAccountingPeriodDto.toLedgerResponse(): LedgerAccountingPeriodResponse =
    LedgerAccountingPeriodResponse(
        periodId = periodId,
        code = code,
        startDate = startDate,
        endDate = endDate,
        status = status,
    )

private fun PostJournalEntryLineRequest.toAssignments(): DimensionAssignments =
    DimensionAssignments(
        costCenterId = costCenterId,
        profitCenterId = profitCenterId,
        departmentId = departmentId,
        projectId = projectId,
        businessAreaId = businessAreaId,
    )
