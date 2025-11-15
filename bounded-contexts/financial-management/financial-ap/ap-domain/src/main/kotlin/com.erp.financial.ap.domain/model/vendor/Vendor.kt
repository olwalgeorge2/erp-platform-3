package com.erp.financial.ap.domain.model.vendor

import com.erp.financial.shared.masterdata.MasterDataStatus
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class Vendor(
    val id: VendorId,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val vendorNumber: VendorNumber,
    val status: MasterDataStatus,
    val profile: VendorProfile,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun activate(clock: Clock = Clock.systemUTC()): Vendor =
        if (status == MasterDataStatus.ACTIVE) {
            this
        } else {
            copy(status = MasterDataStatus.ACTIVE, updatedAt = Instant.now(clock))
        }

    fun deactivate(clock: Clock = Clock.systemUTC()): Vendor =
        if (status == MasterDataStatus.INACTIVE) {
            this
        } else {
            copy(status = MasterDataStatus.INACTIVE, updatedAt = Instant.now(clock))
        }

    fun updateProfile(
        profile: VendorProfile,
        clock: Clock = Clock.systemUTC(),
    ): Vendor = copy(profile = profile, updatedAt = Instant.now(clock))

    fun hasDimensionDefaults(): Boolean = !profile.dimensionDefaults.isEmpty()

    companion object {
        fun register(
            tenantId: UUID,
            companyCodeId: UUID,
            vendorNumber: VendorNumber,
            profile: VendorProfile,
            clock: Clock = Clock.systemUTC(),
        ): Vendor {
            val now = Instant.now(clock)
            return Vendor(
                id = VendorId.newId(),
                tenantId = tenantId,
                companyCodeId = companyCodeId,
                vendorNumber = vendorNumber,
                status = MasterDataStatus.ACTIVE,
                profile = profile,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
