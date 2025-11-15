package com.erp.financial.ap.application.port.input

import com.erp.financial.ap.application.port.input.command.CreateVendorBillCommand
import com.erp.financial.ap.application.port.input.command.PostVendorBillCommand
import com.erp.financial.ap.application.port.input.command.RecordVendorPaymentCommand
import com.erp.financial.ap.application.port.input.query.ListVendorBillsQuery
import com.erp.financial.ap.application.port.input.query.VendorBillDetailQuery
import com.erp.financial.ap.domain.model.bill.VendorBill

interface BillCommandUseCase {
    fun createBill(command: CreateVendorBillCommand): VendorBill

    fun postBill(command: PostVendorBillCommand): VendorBill

    fun recordPayment(command: RecordVendorPaymentCommand): VendorBill

    fun listBills(query: ListVendorBillsQuery): List<VendorBill>

    fun getBill(query: VendorBillDetailQuery): VendorBill?
}
