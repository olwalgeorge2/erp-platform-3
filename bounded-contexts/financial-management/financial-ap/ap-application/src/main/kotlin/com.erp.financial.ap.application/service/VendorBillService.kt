package com.erp.financial.ap.application.service

import com.erp.financial.ap.application.port.input.BillCommandUseCase
import com.erp.financial.ap.application.port.input.command.CreateVendorBillCommand
import com.erp.financial.ap.application.port.input.command.PostVendorBillCommand
import com.erp.financial.ap.application.port.input.command.RecordVendorPaymentCommand
import com.erp.financial.ap.application.port.input.query.ListVendorBillsQuery
import com.erp.financial.ap.application.port.input.query.VendorBillDetailQuery
import com.erp.financial.ap.application.port.output.ApOpenItemRepository
import com.erp.financial.ap.application.port.output.BillRepository
import com.erp.financial.ap.application.port.output.FinancialPostingPort
import com.erp.financial.ap.application.port.output.VendorRepository
import com.erp.financial.ap.domain.model.bill.BillId
import com.erp.financial.ap.domain.model.bill.BillLine
import com.erp.financial.ap.domain.model.bill.BillStatus
import com.erp.financial.ap.domain.model.bill.VendorBill
import com.erp.financial.ap.domain.model.vendor.VendorId
import com.erp.financial.shared.Money
import jakarta.enterprise.context.ApplicationScoped
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
class VendorBillService
    @jakarta.inject.Inject
    constructor(
        private val billRepository: BillRepository,
        private val vendorRepository: VendorRepository,
        private val financialPostingPort: FinancialPostingPort,
        private val openItemRepository: ApOpenItemRepository,
        private val clock: Clock,
    ) : BillCommandUseCase {
        override fun createBill(command: CreateVendorBillCommand): VendorBill {
            val vendor =
                vendorRepository.findById(command.tenantId, VendorId(command.vendorId))
                    ?: throw IllegalArgumentException("Vendor not found")
            val currency = command.currency.uppercase()
            val defaultDimensions =
                if (command.dimensionAssignments.isEmpty() && !vendor.profile.dimensionDefaults.isEmpty()) {
                    vendor.profile.dimensionDefaults
                } else {
                    command.dimensionAssignments
                }
            val lines =
                command.lines.map {
                    BillLine(
                        glAccountId = it.glAccountId,
                        description = it.description,
                        netAmount = Money(it.netAmount, currency),
                        taxAmount = Money(it.taxAmount, currency),
                        dimensionAssignments = it.dimensionAssignments,
                    )
                }
            val bill =
                VendorBill.draft(
                    tenantId = command.tenantId,
                    companyCodeId = command.companyCodeId,
                    vendorId = command.vendorId,
                    invoiceNumber = command.invoiceNumber,
                    invoiceDate = command.invoiceDate,
                    dueDate = command.dueDate,
                    currency = currency,
                    lines = lines,
                    dimensionAssignments = defaultDimensions,
                    taxAmount = Money(command.lines.sumOf { it.taxAmount }, currency),
                    paymentTerms = vendor.profile.paymentTerms,
                    clock = clock,
                )
            val approved = bill.approve(clock)
            return billRepository.save(approved)
        }

        override fun postBill(command: PostVendorBillCommand): VendorBill {
            val bill = loadBill(command.tenantId, command.billId)
            if (bill.status == BillStatus.POSTED && bill.journalEntryId != null) {
                return bill
            }
            val updated = bill.post(clock)
            val postingResult = financialPostingPort.postVendorBill(updated)
            val withJournal = updated.assignJournalEntry(postingResult.journalEntryId, clock)
            return billRepository.save(withJournal)
        }

        override fun recordPayment(command: RecordVendorPaymentCommand): VendorBill {
            val bill = loadBill(command.tenantId, command.billId)
            val currency = bill.currency
            val paymentDate = command.paymentDate ?: LocalDate.now(clock)
            val updated = bill.applyPayment(Money(command.paymentAmount, currency), clock)
            val saved = billRepository.save(updated)
            openItemRepository.updatePaymentMetadata(saved.id.value, paymentDate)
            return saved
        }

        override fun listBills(query: ListVendorBillsQuery): List<VendorBill> =
            billRepository.list(query.tenantId, query.companyCodeId, query.vendorId, query.status, query.dueBefore)

        override fun getBill(query: VendorBillDetailQuery): VendorBill? =
            billRepository.findById(query.tenantId, BillId(query.billId))

        private fun loadBill(
            tenantId: UUID,
            billId: UUID,
        ): VendorBill =
            billRepository.findById(tenantId, BillId(billId))
                ?: throw IllegalArgumentException("Bill not found")
    }
