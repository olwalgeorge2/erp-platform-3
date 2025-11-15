package com.erp.financial.ap.infrastructure.adapter.output.persistence

import com.erp.financial.ap.application.port.output.VendorRepository
import com.erp.financial.ap.domain.model.vendor.Vendor
import com.erp.financial.ap.domain.model.vendor.VendorId
import com.erp.financial.ap.domain.model.vendor.VendorNumber
import com.erp.financial.ap.infrastructure.persistence.entity.VendorEntity
import com.erp.financial.shared.masterdata.MasterDataStatus
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaVendorRepository
    @Inject
    constructor(
        private val entityManager: EntityManager,
    ) : VendorRepository {
        override fun save(vendor: Vendor): Vendor {
            val managed = entityManager.find(VendorEntity::class.java, vendor.id.value)
            val entity =
                if (managed == null) {
                    VendorEntity.from(vendor).also { entityManager.persist(it) }
                } else {
                    managed.updateFrom(vendor)
                    managed
                }
            entityManager.flush()
            return entity.toDomain()
        }

        override fun findById(
            tenantId: UUID,
            vendorId: VendorId,
        ): Vendor? =
            entityManager
                .find(VendorEntity::class.java, vendorId.value)
                ?.takeIf { it.tenantId == tenantId }
                ?.toDomain()

        override fun findByVendorNumber(
            tenantId: UUID,
            vendorNumber: VendorNumber,
        ): Vendor? =
            entityManager
                .createQuery(
                    "SELECT v FROM VendorEntity v WHERE v.tenantId = :tenantId AND v.vendorNumber = :vendorNumber",
                    VendorEntity::class.java,
                ).setParameter("tenantId", tenantId)
                .setParameter("vendorNumber", vendorNumber.value)
                .resultList
                .firstOrNull()
                ?.toDomain()

        override fun delete(
            tenantId: UUID,
            vendorId: VendorId,
        ) {
            val entity = entityManager.find(VendorEntity::class.java, vendorId.value) ?: return
            if (entity.tenantId != tenantId) {
                return
            }
            entityManager.remove(entity)
        }

        override fun list(
            tenantId: UUID,
            companyCodeId: UUID?,
            status: MasterDataStatus?,
        ): List<Vendor> {
            val jpql = StringBuilder("SELECT v FROM VendorEntity v WHERE v.tenantId = :tenantId")
            if (companyCodeId != null) {
                jpql.append(" AND v.companyCodeId = :companyCodeId")
            }
            if (status != null) {
                jpql.append(" AND v.status = :status")
            }
            jpql.append(" ORDER BY v.vendorNumber ASC")

            val query =
                entityManager
                    .createQuery(jpql.toString(), VendorEntity::class.java)
                    .setParameter("tenantId", tenantId)
            companyCodeId?.let { query.setParameter("companyCodeId", it) }
            status?.let { query.setParameter("status", it) }

            return query.resultList.map(VendorEntity::toDomain)
        }
    }
