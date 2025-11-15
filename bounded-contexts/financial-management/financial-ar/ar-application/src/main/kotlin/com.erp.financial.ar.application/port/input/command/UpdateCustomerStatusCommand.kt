package com.erp.financial.ar.application.port.input.command

import com.erp.financial.shared.masterdata.MasterDataStatus
import java.util.UUID

data class UpdateCustomerStatusCommand(
    val tenantId: UUID,
    val customerId: UUID,
    val targetStatus: MasterDataStatus,
)
