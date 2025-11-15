package com.erp.financial.ap.application.port.input.command

import com.erp.financial.ap.domain.model.vendor.VendorNumber
import com.erp.financial.ap.domain.model.vendor.VendorProfile
import java.util.UUID

data class RegisterVendorCommand(
    val tenantId: UUID,
    val companyCodeId: UUID,
    val vendorNumber: VendorNumber,
    val profile: VendorProfile,
)
