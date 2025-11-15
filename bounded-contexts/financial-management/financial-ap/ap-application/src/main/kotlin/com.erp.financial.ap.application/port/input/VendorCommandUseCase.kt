package com.erp.financial.ap.application.port.input

import com.erp.financial.ap.application.port.input.command.RegisterVendorCommand
import com.erp.financial.ap.application.port.input.command.UpdateVendorCommand
import com.erp.financial.ap.application.port.input.command.UpdateVendorStatusCommand
import com.erp.financial.ap.application.port.input.query.ListVendorsQuery
import com.erp.financial.ap.application.port.input.query.VendorDetailQuery
import com.erp.financial.ap.domain.model.vendor.Vendor
import java.util.UUID

interface VendorCommandUseCase {
    fun registerVendor(command: RegisterVendorCommand): Vendor

    fun updateVendor(command: UpdateVendorCommand): Vendor

    fun updateVendorStatus(command: UpdateVendorStatusCommand): Vendor

    fun listVendors(query: ListVendorsQuery): List<Vendor>

    fun getVendor(query: VendorDetailQuery): Vendor?

    fun deleteVendor(
        tenantId: UUID,
        vendorId: UUID,
    )
}
