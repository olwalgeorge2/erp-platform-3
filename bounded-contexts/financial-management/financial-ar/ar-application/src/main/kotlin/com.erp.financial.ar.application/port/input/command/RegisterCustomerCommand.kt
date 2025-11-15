package com.erp.financial.ar.application.port.input.command

import com.erp.financial.ar.domain.model.customer.CustomerNumber
import com.erp.financial.ar.domain.model.customer.CustomerProfile
import java.util.UUID

data class RegisterCustomerCommand(
    val tenantId: UUID,
    val companyCodeId: UUID,
    val customerNumber: CustomerNumber,
    val profile: CustomerProfile,
)
