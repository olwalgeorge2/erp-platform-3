package com.erp.financial.ar.infrastructure.adapter.input.rest

import com.erp.financial.ar.application.port.input.CustomerCommandUseCase
import com.erp.financial.ar.application.port.input.CustomerInvoiceUseCase
import com.erp.financial.ar.application.port.input.command.CreateCustomerInvoiceCommand
import com.erp.financial.ar.application.port.input.command.PostCustomerInvoiceCommand
import com.erp.financial.ar.application.port.input.command.RecordCustomerReceiptCommand
import com.erp.financial.ar.application.port.input.command.RegisterCustomerCommand
import com.erp.financial.ar.application.port.input.command.UpdateCustomerCommand
import com.erp.financial.ar.application.port.input.command.UpdateCustomerStatusCommand
import com.erp.financial.ar.application.port.input.query.CustomerDetailQuery
import com.erp.financial.ar.application.port.input.query.CustomerInvoiceDetailQuery
import com.erp.financial.ar.application.port.input.query.ListCustomerInvoicesQuery
import com.erp.financial.ar.application.port.input.query.ListCustomersQuery
import com.erp.financial.ar.domain.model.customer.Customer
import com.erp.financial.ar.domain.model.invoice.CustomerInvoice
import io.quarkus.test.Mock
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@Mock
@ApplicationScoped
class MockCustomerCommandUseCase : CustomerCommandUseCase {
    override fun registerCustomer(command: RegisterCustomerCommand): Customer =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun updateCustomer(command: UpdateCustomerCommand): Customer =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun updateCustomerStatus(command: UpdateCustomerStatusCommand): Customer =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun listCustomers(query: ListCustomersQuery): List<Customer> =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun getCustomer(query: CustomerDetailQuery): Customer? =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun deleteCustomer(
        tenantId: UUID,
        customerId: UUID,
    ): Unit = throw UnsupportedOperationException("Should not be invoked during validation tests.")
}

@Mock
@ApplicationScoped
class MockCustomerInvoiceUseCase : CustomerInvoiceUseCase {
    override fun createInvoice(command: CreateCustomerInvoiceCommand): CustomerInvoice =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun postInvoice(command: PostCustomerInvoiceCommand): CustomerInvoice =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun recordReceipt(command: RecordCustomerReceiptCommand): CustomerInvoice =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun listInvoices(query: ListCustomerInvoicesQuery): List<CustomerInvoice> =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun getInvoice(query: CustomerInvoiceDetailQuery): CustomerInvoice? =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")
}
