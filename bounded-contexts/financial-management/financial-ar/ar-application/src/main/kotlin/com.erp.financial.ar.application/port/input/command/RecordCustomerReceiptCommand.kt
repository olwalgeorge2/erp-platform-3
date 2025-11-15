package com.erp.financial.ar.application.port.input.command

import java.time.LocalDate
import java.util.UUID

data class RecordCustomerReceiptCommand(
    val tenantId: UUID,
    val invoiceId: UUID,
    val receiptAmount: Long,
    val receiptDate: LocalDate? = null,
)
