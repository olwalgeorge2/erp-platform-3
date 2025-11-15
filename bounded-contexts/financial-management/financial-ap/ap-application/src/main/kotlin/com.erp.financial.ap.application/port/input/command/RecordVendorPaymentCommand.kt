package com.erp.financial.ap.application.port.input.command

import java.time.LocalDate
import java.util.UUID

data class RecordVendorPaymentCommand(
    val tenantId: UUID,
    val billId: UUID,
    val paymentAmount: Long,
    val paymentDate: LocalDate? = null,
)
