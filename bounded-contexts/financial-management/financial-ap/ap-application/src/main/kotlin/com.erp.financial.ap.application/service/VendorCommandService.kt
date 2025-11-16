package com.erp.financial.ap.application.service

import com.erp.financial.ap.application.cache.VendorExistenceCache
import com.erp.financial.ap.application.port.input.VendorCommandUseCase
import com.erp.financial.ap.application.port.input.command.RegisterVendorCommand
import com.erp.financial.ap.application.port.input.command.UpdateVendorCommand
import com.erp.financial.ap.application.port.input.command.UpdateVendorStatusCommand
import com.erp.financial.ap.application.port.input.query.ListVendorsQuery
import com.erp.financial.ap.application.port.input.query.VendorDetailQuery
import com.erp.financial.ap.application.port.output.VendorRepository
import com.erp.financial.ap.domain.model.vendor.Vendor
import com.erp.financial.ap.domain.model.vendor.VendorId
import com.erp.financial.shared.masterdata.MasterDataStatus
import jakarta.enterprise.context.ApplicationScoped
import java.time.Clock
import java.util.UUID

@ApplicationScoped
class VendorCommandService(
    private val vendorRepository: VendorRepository,
    private val clock: Clock,
    private val vendorExistenceCache: VendorExistenceCache,
) : VendorCommandUseCase {
    override fun registerVendor(command: RegisterVendorCommand): Vendor {
        vendorRepository
            .findByVendorNumber(command.tenantId, command.vendorNumber)
            ?.let { throw IllegalStateException("Vendor number already used for tenant") }

        val vendor =
            Vendor.register(
                tenantId = command.tenantId,
                companyCodeId = command.companyCodeId,
                vendorNumber = command.vendorNumber,
                profile = command.profile,
                clock = clock,
            )
        val saved = vendorRepository.save(vendor)
        vendorExistenceCache.put(saved)
        return saved
    }

    override fun updateVendor(command: UpdateVendorCommand): Vendor {
        val vendorId = VendorId(command.vendorId)
        val existing =
            vendorExistenceCache.find(command.tenantId, command.vendorId)
                ?: throw IllegalArgumentException("Vendor not found")
        val updated = existing.updateProfile(command.profile, clock)
        val saved = vendorRepository.save(updated)
        vendorExistenceCache.put(saved)
        return saved
    }

    override fun updateVendorStatus(command: UpdateVendorStatusCommand): Vendor {
        val vendorId = VendorId(command.vendorId)
        val existing =
            vendorExistenceCache.find(command.tenantId, command.vendorId)
                ?: throw IllegalArgumentException("Vendor not found")
        val updated =
            when (command.targetStatus) {
                MasterDataStatus.ACTIVE -> existing.activate(clock)
                MasterDataStatus.INACTIVE -> existing.deactivate(clock)
            }
        val saved = vendorRepository.save(updated)
        vendorExistenceCache.put(saved)
        return saved
    }

    override fun listVendors(query: ListVendorsQuery): List<Vendor> =
        vendorRepository.list(query.tenantId, query.companyCodeId, query.status)

    override fun getVendor(query: VendorDetailQuery): Vendor? =
        vendorExistenceCache.find(query.tenantId, query.vendorId)

    override fun deleteVendor(
        tenantId: UUID,
        vendorId: UUID,
    ) {
        vendorRepository.delete(tenantId, VendorId(vendorId))
        vendorExistenceCache.evict(tenantId, vendorId)
    }
}
