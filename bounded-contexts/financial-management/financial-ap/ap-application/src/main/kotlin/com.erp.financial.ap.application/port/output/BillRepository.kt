package com.erp.financial.ap.application.port.output

import com.erp.financial.ap.domain.model.bill.BillId
import com.erp.financial.ap.domain.model.bill.BillStatus
import com.erp.financial.ap.domain.model.bill.VendorBill
import java.time.LocalDate
import java.util.UUID

interface BillRepository {
    fun save(bill: VendorBill): VendorBill

    fun findById(
        tenantId: UUID,
        billId: BillId,
    ): VendorBill?

    fun list(
        tenantId: UUID,
        companyCodeId: UUID?,
        vendorId: UUID?,
        status: BillStatus?,
        dueBefore: LocalDate?,
    ): List<VendorBill>
}
