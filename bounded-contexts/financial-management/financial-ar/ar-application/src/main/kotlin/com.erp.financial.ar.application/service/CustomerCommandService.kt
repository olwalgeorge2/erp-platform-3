package com.erp.financial.ar.application.service

import com.erp.financial.ar.application.cache.CustomerExistenceCache
import com.erp.financial.ar.application.port.input.CustomerCommandUseCase
import com.erp.financial.ar.application.port.input.command.RegisterCustomerCommand
import com.erp.financial.ar.application.port.input.command.UpdateCustomerCommand
import com.erp.financial.ar.application.port.input.command.UpdateCustomerStatusCommand
import com.erp.financial.ar.application.port.input.query.CustomerDetailQuery
import com.erp.financial.ar.application.port.input.query.ListCustomersQuery
import com.erp.financial.ar.application.port.output.CustomerRepository
import com.erp.financial.ar.domain.model.customer.Customer
import com.erp.financial.ar.domain.model.customer.CustomerId
import com.erp.financial.shared.masterdata.MasterDataStatus
import jakarta.enterprise.context.ApplicationScoped
import java.time.Clock
import java.util.UUID

@ApplicationScoped
class CustomerCommandService(
    private val customerRepository: CustomerRepository,
    private val clock: Clock,
    private val customerExistenceCache: CustomerExistenceCache,
) : CustomerCommandUseCase {
    override fun registerCustomer(command: RegisterCustomerCommand): Customer {
        customerRepository
            .findByNumber(command.tenantId, command.customerNumber)
            ?.let { throw IllegalStateException("Customer number already exists for tenant") }

        val customer =
            Customer.register(
                tenantId = command.tenantId,
                companyCodeId = command.companyCodeId,
                customerNumber = command.customerNumber,
                profile = command.profile,
                clock = clock,
            )
        val saved = customerRepository.save(customer)
        customerExistenceCache.put(saved)
        return saved
    }

    override fun updateCustomer(command: UpdateCustomerCommand): Customer {
        val id = CustomerId(command.customerId)
        val existing =
            customerExistenceCache.find(command.tenantId, command.customerId)
                ?: throw IllegalArgumentException("Customer not found")
        val updated = existing.updateProfile(command.profile, clock)
        val saved = customerRepository.save(updated)
        customerExistenceCache.put(saved)
        return saved
    }

    override fun updateCustomerStatus(command: UpdateCustomerStatusCommand): Customer {
        val id = CustomerId(command.customerId)
        val customer =
            customerExistenceCache.find(command.tenantId, command.customerId)
                ?: throw IllegalArgumentException("Customer not found")
        val updated =
            when (command.targetStatus) {
                MasterDataStatus.ACTIVE -> customer.activate(clock)
                MasterDataStatus.INACTIVE -> customer.deactivate(clock)
            }
        val saved = customerRepository.save(updated)
        customerExistenceCache.put(saved)
        return saved
    }

    override fun listCustomers(query: ListCustomersQuery): List<Customer> =
        customerRepository.list(query.tenantId, query.companyCodeId, query.status)

    override fun getCustomer(query: CustomerDetailQuery): Customer? =
        customerExistenceCache.find(query.tenantId, query.customerId)

    override fun deleteCustomer(
        tenantId: UUID,
        customerId: UUID,
    ) {
        validationCircuitBreaker.guard("customer_delete") {
        customerRepository.delete(tenantId, CustomerId(customerId))
        customerExistenceCache.evict(tenantId, customerId)
    }
}
}
