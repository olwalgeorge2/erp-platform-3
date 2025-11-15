package com.erp.financial.ar.application.port.input.query

import java.util.UUID

data class CustomerDetailQuery(
    val tenantId: UUID,
    val customerId: UUID,
)
