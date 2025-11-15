package com.erp.financial.ar.application.port.input

import com.erp.financial.ar.application.port.input.command.CreateCustomerInvoiceCommand
import com.erp.financial.ar.application.port.input.command.PostCustomerInvoiceCommand
import com.erp.financial.ar.application.port.input.command.RecordCustomerReceiptCommand
import com.erp.financial.ar.application.port.input.query.CustomerInvoiceDetailQuery
import com.erp.financial.ar.application.port.input.query.ListCustomerInvoicesQuery
import com.erp.financial.ar.domain.model.invoice.CustomerInvoice

interface CustomerInvoiceUseCase {
    fun createInvoice(command: CreateCustomerInvoiceCommand): CustomerInvoice

    fun postInvoice(command: PostCustomerInvoiceCommand): CustomerInvoice

    fun recordReceipt(command: RecordCustomerReceiptCommand): CustomerInvoice

    fun listInvoices(query: ListCustomerInvoicesQuery): List<CustomerInvoice>

    fun getInvoice(query: CustomerInvoiceDetailQuery): CustomerInvoice?
}
