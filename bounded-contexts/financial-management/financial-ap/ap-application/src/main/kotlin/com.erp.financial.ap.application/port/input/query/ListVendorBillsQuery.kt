package com.erp.financial.ap.application.port.input.query

import com.erp.financial.ap.domain.model.bill.BillStatus
import java.time.LocalDate
import java.util.UUID

data class ListVendorBillsQuery(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val vendorId: UUID? = null,
    val status: BillStatus? = null,
    val dueBefore: LocalDate? = null,
)
