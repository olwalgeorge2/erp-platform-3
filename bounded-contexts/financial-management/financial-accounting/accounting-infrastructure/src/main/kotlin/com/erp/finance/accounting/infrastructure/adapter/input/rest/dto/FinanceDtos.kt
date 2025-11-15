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
import io.quarkus.runtime.annotations.RegisterForReflection
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import com.erp.finance.accounting.application.port.input.dto.AccountingPeriodDto as QueryAccountingPeriodDto

@RegisterForReflection
data class CreateLedgerRequest(
    val tenantId: UUID,
    val chartOfAccountsId: UUID,
    val baseCurrency: String,
    val chartCode: String? = null,
    val chartName: String? = null,
    val reference: String? = null,
) {
    fun toCommand(): CreateLedgerCommand =
        CreateLedgerCommand(
            tenantId = tenantId,
            chartOfAccountsId = chartOfAccountsId,
            baseCurrency = baseCurrency.uppercase(),
            chartCode = chartCode?.takeIf { it.isNotBlank() } ?: "DEFAULT",
            chartName = chartName?.takeIf { it.isNotBlank() } ?: "Default Chart",
            reference = reference,
        )
}

@RegisterForReflection
data class DefineAccountRequest(
    val tenantId: UUID,
    val parentAccountId: UUID? = null,
    val code: String,
    val name: String,
    val type: AccountType,
    val currency: String? = null,
    val isPosting: Boolean = true,
) {
    fun toCommand(chartOfAccountsId: UUID): DefineAccountCommand =
        DefineAccountCommand(
            tenantId = tenantId,
            chartOfAccountsId = chartOfAccountsId,
            parentAccountId = parentAccountId,
            code = code,
            name = name,
            type = type,
            currency = currency?.uppercase(),
            isPosting = isPosting,
        )
}

@RegisterForReflection
data class PostJournalEntryRequest(
    val tenantId: UUID,
    val ledgerId: UUID,
    val accountingPeriodId: UUID,
    val reference: String? = null,
    val description: String? = null,
    val bookedAt: Instant? = null,
    val lines: List<PostJournalEntryLineRequest>,
) {
    fun toCommand(): PostJournalEntryCommand =
        PostJournalEntryCommand(
            tenantId = tenantId,
            ledgerId = ledgerId,
            accountingPeriodId = accountingPeriodId,
            reference = reference,
            description = description,
            bookedAt = bookedAt ?: Instant.now(),
            lines =
                lines.map { line ->
                    JournalEntryLineCommand(
                        accountId = AccountId(line.accountId),
                        direction = line.direction,
                        amount = Money(line.amountMinor),
                        currency = line.currency?.uppercase(),
                        description = line.description,
                        dimensions = line.toAssignments(),
                    )
                },
        )
}

@RegisterForReflection
data class PostJournalEntryLineRequest(
    val accountId: UUID,
    val direction: EntryDirection,
    @Schema(description = "Monetary amount expressed in minor units (e.g. cents)")
    val amountMinor: Long,
    val currency: String? = null,
    val description: String? = null,
    val costCenterId: UUID? = null,
    val profitCenterId: UUID? = null,
    val departmentId: UUID? = null,
    val projectId: UUID? = null,
    val businessAreaId: UUID? = null,
)

@RegisterForReflection
data class ClosePeriodRequest(
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
    val tenantId: UUID,
    val gainAccountId: UUID,
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

data class LedgerAccountingPeriodResponse(
    val periodId: UUID,
    val code: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: String,
)

@Schema(
    name = "ErrorResponse",
    description = "Financial accounting service error payload",
)
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, Any?> = emptyMap(),
)

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
