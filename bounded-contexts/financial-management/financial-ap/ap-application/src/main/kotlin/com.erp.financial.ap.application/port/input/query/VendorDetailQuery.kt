package com.erp.financial.ap.application.port.input.query

import java.util.UUID

data class VendorDetailQuery(
    val tenantId: UUID,
    val vendorId: UUID,
)
