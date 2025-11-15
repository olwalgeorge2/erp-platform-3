package com.erp.financial.ar.application.port.input.command

import java.util.UUID

data class PostCustomerInvoiceCommand(
    val tenantId: UUID,
    val invoiceId: UUID,
)
