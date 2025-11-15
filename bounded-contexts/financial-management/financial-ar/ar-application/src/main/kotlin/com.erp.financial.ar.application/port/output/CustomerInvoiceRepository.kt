package com.erp.financial.ar.application.port.output

import com.erp.financial.ar.domain.model.invoice.CustomerInvoice
import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceId
import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceStatus
import java.time.LocalDate
import java.util.UUID

interface CustomerInvoiceRepository {
    fun save(invoice: CustomerInvoice): CustomerInvoice

    fun findById(
        tenantId: UUID,
        invoiceId: CustomerInvoiceId,
    ): CustomerInvoice?

    fun list(
        tenantId: UUID,
        companyCodeId: UUID? = null,
        customerId: UUID? = null,
        status: CustomerInvoiceStatus? = null,
        dueBefore: LocalDate? = null,
    ): List<CustomerInvoice>
}
