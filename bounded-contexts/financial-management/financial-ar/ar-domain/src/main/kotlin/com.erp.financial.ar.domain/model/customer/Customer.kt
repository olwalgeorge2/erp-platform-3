package com.erp.financial.ar.domain.model.customer

import com.erp.financial.shared.masterdata.MasterDataStatus
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class Customer(
    val id: CustomerId,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val customerNumber: CustomerNumber,
    val profile: CustomerProfile,
    val status: MasterDataStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun updateProfile(
        profile: CustomerProfile,
        clock: Clock = Clock.systemUTC(),
    ): Customer = copy(profile = profile, updatedAt = Instant.now(clock))

    fun activate(clock: Clock = Clock.systemUTC()): Customer =
        if (status == MasterDataStatus.ACTIVE) {
            this
        } else {
            copy(status = MasterDataStatus.ACTIVE, updatedAt = Instant.now(clock))
        }

    fun deactivate(clock: Clock = Clock.systemUTC()): Customer =
        if (status == MasterDataStatus.INACTIVE) {
            this
        } else {
            copy(status = MasterDataStatus.INACTIVE, updatedAt = Instant.now(clock))
        }

    companion object {
        fun register(
            tenantId: UUID,
            companyCodeId: UUID,
            customerNumber: CustomerNumber,
            profile: CustomerProfile,
            clock: Clock = Clock.systemUTC(),
        ): Customer {
            val now = Instant.now(clock)
            return Customer(
                id = CustomerId.newId(),
                tenantId = tenantId,
                companyCodeId = companyCodeId,
                customerNumber = customerNumber,
                profile = profile,
                status = MasterDataStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
