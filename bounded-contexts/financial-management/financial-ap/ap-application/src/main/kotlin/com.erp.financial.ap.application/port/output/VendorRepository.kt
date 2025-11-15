package com.erp.financial.ap.application.port.output

import com.erp.financial.ap.domain.model.vendor.Vendor
import com.erp.financial.ap.domain.model.vendor.VendorId
import com.erp.financial.ap.domain.model.vendor.VendorNumber
import com.erp.financial.shared.masterdata.MasterDataStatus
import java.util.UUID

interface VendorRepository {
    fun save(vendor: Vendor): Vendor

    fun findById(
        tenantId: UUID,
        vendorId: VendorId,
    ): Vendor?

    fun findByVendorNumber(
        tenantId: UUID,
        vendorNumber: VendorNumber,
    ): Vendor?

    fun delete(
        tenantId: UUID,
        vendorId: VendorId,
    )

    fun list(
        tenantId: UUID,
        companyCodeId: UUID?,
        status: MasterDataStatus?,
    ): List<Vendor>
}
