package com.erp.financial.ar.application.port.input

import com.erp.financial.ar.application.port.input.command.RegisterCustomerCommand
import com.erp.financial.ar.application.port.input.command.UpdateCustomerCommand
import com.erp.financial.ar.application.port.input.command.UpdateCustomerStatusCommand
import com.erp.financial.ar.application.port.input.query.CustomerDetailQuery
import com.erp.financial.ar.application.port.input.query.ListCustomersQuery
import com.erp.financial.ar.domain.model.customer.Customer
import java.util.UUID

interface CustomerCommandUseCase {
    fun registerCustomer(command: RegisterCustomerCommand): Customer

    fun updateCustomer(command: UpdateCustomerCommand): Customer

    fun updateCustomerStatus(command: UpdateCustomerStatusCommand): Customer

    fun listCustomers(query: ListCustomersQuery): List<Customer>

    fun getCustomer(query: CustomerDetailQuery): Customer?

    fun deleteCustomer(
        tenantId: UUID,
        customerId: UUID,
    )
}
