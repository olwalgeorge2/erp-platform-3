package com.erp.financial.ap.application.port.input.command

import java.util.UUID

data class PostVendorBillCommand(
    val tenantId: UUID,
    val billId: UUID,
)
