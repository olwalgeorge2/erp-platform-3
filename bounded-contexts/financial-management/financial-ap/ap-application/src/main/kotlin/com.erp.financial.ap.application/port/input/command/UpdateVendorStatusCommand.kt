package com.erp.financial.ap.application.port.input.command

import com.erp.financial.shared.masterdata.MasterDataStatus
import java.util.UUID

data class UpdateVendorStatusCommand(
    val tenantId: UUID,
    val vendorId: UUID,
    val targetStatus: MasterDataStatus,
)
