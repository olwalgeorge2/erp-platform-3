package com.erp.financial.ar.infrastructure.adapter.output.persistence

import com.erp.financial.ar.application.port.output.CustomerRepository
import com.erp.financial.ar.domain.model.customer.Customer
import com.erp.financial.ar.domain.model.customer.CustomerId
import com.erp.financial.ar.domain.model.customer.CustomerNumber
import com.erp.financial.ar.infrastructure.persistence.entity.CustomerEntity
import com.erp.financial.shared.masterdata.MasterDataStatus
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaCustomerRepository
    @Inject
    constructor(
        private val entityManager: EntityManager,
    ) : CustomerRepository {
        override fun save(customer: Customer): Customer {
            val managed = entityManager.find(CustomerEntity::class.java, customer.id.value)
            val entity =
                if (managed == null) {
                    CustomerEntity.from(customer).also { entityManager.persist(it) }
                } else {
                    managed.updateFrom(customer)
                    managed
                }
            entityManager.flush()
            return entity.toDomain()
        }

        override fun findById(
            tenantId: UUID,
            id: CustomerId,
        ): Customer? =
            entityManager
                .find(CustomerEntity::class.java, id.value)
                ?.takeIf { it.tenantId == tenantId }
                ?.toDomain()

        override fun findByNumber(
            tenantId: UUID,
            customerNumber: CustomerNumber,
        ): Customer? =
            entityManager
                .createQuery(
                    "SELECT c FROM CustomerEntity c WHERE c.tenantId = :tenantId AND c.customerNumber = :customerNumber",
                    CustomerEntity::class.java,
                ).setParameter("tenantId", tenantId)
                .setParameter("customerNumber", customerNumber.value)
                .resultList
                .firstOrNull()
                ?.toDomain()

        override fun delete(
            tenantId: UUID,
            id: CustomerId,
        ) {
            val entity = entityManager.find(CustomerEntity::class.java, id.value) ?: return
            if (entity.tenantId != tenantId) {
                return
            }
            entityManager.remove(entity)
        }

        override fun list(
            tenantId: UUID,
            companyCodeId: UUID?,
            status: MasterDataStatus?,
        ): List<Customer> {
            val builder = StringBuilder("SELECT c FROM CustomerEntity c WHERE c.tenantId = :tenantId")
            if (companyCodeId != null) {
                builder.append(" AND c.companyCodeId = :companyCodeId")
            }
            if (status != null) {
                builder.append(" AND c.status = :status")
            }
            builder.append(" ORDER BY c.customerNumber ASC")

            val query =
                entityManager
                    .createQuery(builder.toString(), CustomerEntity::class.java)
                    .setParameter("tenantId", tenantId)
            companyCodeId?.let { query.setParameter("companyCodeId", it) }
            status?.let { query.setParameter("status", it) }
            return query.resultList.map(CustomerEntity::toDomain)
        }
    }
