package com.erp.financial.ar.application.port.input.command

import com.erp.financial.ar.domain.model.customer.CustomerProfile
import java.util.UUID

data class UpdateCustomerCommand(
    val tenantId: UUID,
    val customerId: UUID,
    val profile: CustomerProfile,
)
