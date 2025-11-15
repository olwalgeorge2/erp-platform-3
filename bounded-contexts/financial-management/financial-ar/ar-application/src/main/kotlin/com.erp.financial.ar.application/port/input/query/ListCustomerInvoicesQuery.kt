package com.erp.financial.ar.application.port.input.query

import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceStatus
import java.time.LocalDate
import java.util.UUID

data class ListCustomerInvoicesQuery(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val customerId: UUID? = null,
    val status: CustomerInvoiceStatus? = null,
    val dueBefore: LocalDate? = null,
)

data class CustomerInvoiceDetailQuery(
    val tenantId: UUID,
    val invoiceId: UUID,
)
