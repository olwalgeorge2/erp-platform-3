package com.erp.financial.ar.infrastructure.adapter.output

import com.erp.finance.accounting.application.port.input.FinanceCommandUseCase
import com.erp.finance.accounting.application.port.input.command.JournalEntryLineCommand
import com.erp.finance.accounting.application.port.input.command.PostJournalEntryCommand
import com.erp.finance.accounting.application.port.input.query.FinanceQueryUseCase
import com.erp.finance.accounting.application.port.input.query.GetLedgerForCompanyCodeQuery
import com.erp.finance.accounting.application.service.ControlAccountResolutionService
import com.erp.finance.accounting.domain.model.AccountId
import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.finance.accounting.domain.model.EntryDirection
import com.erp.finance.accounting.domain.model.Money
import com.erp.financial.ar.application.port.output.ReceivablePostingPort
import com.erp.financial.ar.application.port.output.ReceivablePostingResult
import com.erp.financial.ar.domain.model.invoice.CustomerInvoice
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ReceivablePostingAdapter
    @jakarta.inject.Inject
    constructor(
        private val financeCommandUseCase: FinanceCommandUseCase,
        private val financeQueryUseCase: FinanceQueryUseCase,
        private val controlAccountResolutionService: ControlAccountResolutionService,
    ) : ReceivablePostingPort {
    override fun postCustomerInvoice(invoice: CustomerInvoice): ReceivablePostingResult {
        val ledgerInfo =
            financeQueryUseCase.getLedgerAndPeriodForCompanyCode(
                GetLedgerForCompanyCodeQuery(
                    companyCodeId = invoice.companyCodeId,
                    tenantId = invoice.tenantId,
                ),
            ) ?: error("No ledger assigned to company code ${invoice.companyCodeId}")
        val period =
            ledgerInfo.currentOpenPeriod
                ?: error("No open period available for ledger ${ledgerInfo.ledgerId}")

        val controlAccountId =
            controlAccountResolutionService.resolveReceivablesAccount(
                tenantId = invoice.tenantId,
                companyCodeId = invoice.companyCodeId,
                dimensionAssignments = invoice.dimensionAssignments,
                currency = invoice.currency,
            )

        val creditLines = buildRevenueLines(invoice)
        val totalCredit = creditLines.sumOf { it.amount.amount }

        val debitLine =
            JournalEntryLineCommand(
                accountId = AccountId(controlAccountId),
                direction = EntryDirection.DEBIT,
                amount = Money(totalCredit),
                currency = invoice.currency,
                description = "AR Control ${invoice.invoiceNumber}",
                dimensions = invoice.dimensionAssignments,
            )

        val command =
            PostJournalEntryCommand(
                tenantId = invoice.tenantId,
                ledgerId = ledgerInfo.ledgerId,
                accountingPeriodId = period.periodId,
                reference = invoice.invoiceNumber,
                description = "Customer invoice ${invoice.invoiceNumber}",
                lines = listOf(debitLine) + creditLines,
            )
        val entry = financeCommandUseCase.postJournalEntry(command)
        return ReceivablePostingResult(
            journalEntryId = entry.id,
            ledgerId = entry.ledgerId.value,
            accountingPeriodId = entry.accountingPeriodId.value,
        )
    }

    private fun buildRevenueLines(invoice: CustomerInvoice): List<JournalEntryLineCommand> =
        invoice.lines.map { line ->
            val amount = line.netAmount.amount + line.taxAmount.amount
            JournalEntryLineCommand(
                accountId = AccountId(line.glAccountId),
                direction = EntryDirection.CREDIT,
                amount = Money(amount),
                currency = invoice.currency,
                description = line.description,
                dimensions = mergeDimensions(invoice.dimensionAssignments, line.dimensionAssignments),
            )
        }

    private fun mergeDimensions(
        header: DimensionAssignments,
        line: DimensionAssignments,
    ): DimensionAssignments =
        DimensionAssignments(
            costCenterId = line.costCenterId ?: header.costCenterId,
            profitCenterId = line.profitCenterId ?: header.profitCenterId,
            departmentId = line.departmentId ?: header.departmentId,
            projectId = line.projectId ?: header.projectId,
            businessAreaId = line.businessAreaId ?: header.businessAreaId,
        )
}
