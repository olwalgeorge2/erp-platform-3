package com.erp.financial.ap.infrastructure.adapter.output

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
import com.erp.financial.ap.application.port.output.FinancialPostingPort
import com.erp.financial.ap.application.port.output.JournalPostingResult
import com.erp.financial.ap.domain.model.bill.VendorBill
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class FinancePostingAdapter
    @jakarta.inject.Inject
    constructor(
        private val financeCommandUseCase: FinanceCommandUseCase,
        private val financeQueryUseCase: FinanceQueryUseCase,
        private val controlAccountResolutionService: ControlAccountResolutionService,
    ) : FinancialPostingPort {
    override fun postVendorBill(bill: VendorBill): JournalPostingResult {
        val ledgerInfo =
            financeQueryUseCase.getLedgerAndPeriodForCompanyCode(
                GetLedgerForCompanyCodeQuery(
                    companyCodeId = bill.companyCodeId,
                    tenantId = bill.tenantId,
                ),
            ) ?: error("No ledger assigned to companyCode=${bill.companyCodeId}")

        val period =
            ledgerInfo.currentOpenPeriod
                ?: error("No open accounting period for ledger ${ledgerInfo.ledgerId}")

        val controlAccountId =
            controlAccountResolutionService.resolvePayablesAccount(
                tenantId = bill.tenantId,
                companyCodeId = bill.companyCodeId,
                dimensionAssignments = bill.dimensionAssignments,
                currency = bill.currency,
            )

        val debitLines = buildDebitLines(bill)
        val totalDebit = debitLines.sumOf { it.amount.amount }
        require(totalDebit > 0) { "Cannot post zero amount vendor bill ${bill.id.value}" }

        val creditLine =
            JournalEntryLineCommand(
                accountId = AccountId(controlAccountId),
                direction = EntryDirection.CREDIT,
                amount = Money(totalDebit),
                currency = bill.currency,
                description = "AP Control ${bill.invoiceNumber}",
                dimensions = bill.dimensionAssignments,
            )

        val command =
            PostJournalEntryCommand(
                tenantId = bill.tenantId,
                ledgerId = ledgerInfo.ledgerId,
                accountingPeriodId = period.periodId,
                reference = bill.invoiceNumber,
                description = "Vendor bill ${bill.invoiceNumber}",
                lines = debitLines + creditLine,
            )
        val entry = financeCommandUseCase.postJournalEntry(command)
        return JournalPostingResult(
            journalEntryId = entry.id,
            ledgerId = entry.ledgerId.value,
            accountingPeriodId = entry.accountingPeriodId.value,
        )
    }

    private fun buildDebitLines(bill: VendorBill): List<JournalEntryLineCommand> =
        bill.lines
            .mapNotNull { line ->
                val amount = line.netAmount.amount + line.taxAmount.amount
                if (amount <= 0) {
                    null
                } else {
                    JournalEntryLineCommand(
                        accountId = AccountId(line.glAccountId),
                        direction = EntryDirection.DEBIT,
                        amount = Money(amount),
                        currency = bill.currency,
                        description = line.description,
                        dimensions = mergeDimensions(bill.dimensionAssignments, line.dimensionAssignments),
                    )
                }
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
