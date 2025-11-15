package com.erp.financial.ap.application.port.input.command

import com.erp.financial.ap.domain.model.vendor.VendorProfile
import java.util.UUID

data class UpdateVendorCommand(
    val tenantId: UUID,
    val vendorId: UUID,
    val profile: VendorProfile,
)
