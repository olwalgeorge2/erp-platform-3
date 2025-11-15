package com.erp.financial.ar.domain.model.invoice

import java.util.UUID

@JvmInline
value class CustomerInvoiceId(
    val value: UUID = UUID.randomUUID(),
)
