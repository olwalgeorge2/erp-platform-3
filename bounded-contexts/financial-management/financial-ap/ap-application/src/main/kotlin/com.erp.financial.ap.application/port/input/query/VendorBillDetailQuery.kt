package com.erp.financial.ap.application.port.input.query

import java.util.UUID

data class VendorBillDetailQuery(
    val tenantId: UUID,
    val billId: UUID,
)
