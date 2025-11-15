package com.erp.financial.ar.application.port.output

import com.erp.financial.ar.domain.model.customer.Customer
import com.erp.financial.ar.domain.model.customer.CustomerId
import com.erp.financial.ar.domain.model.customer.CustomerNumber
import com.erp.financial.shared.masterdata.MasterDataStatus
import java.util.UUID

interface CustomerRepository {
    fun save(customer: Customer): Customer

    fun findById(
        tenantId: UUID,
        id: CustomerId,
    ): Customer?

    fun findByNumber(
        tenantId: UUID,
        customerNumber: CustomerNumber,
    ): Customer?

    fun delete(
        tenantId: UUID,
        id: CustomerId,
    )

    fun list(
        tenantId: UUID,
        companyCodeId: UUID?,
        status: MasterDataStatus?,
    ): List<Customer>
}
