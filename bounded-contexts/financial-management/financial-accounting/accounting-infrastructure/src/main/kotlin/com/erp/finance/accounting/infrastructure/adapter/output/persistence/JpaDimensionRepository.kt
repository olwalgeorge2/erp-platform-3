package com.erp.finance.accounting.infrastructure.adapter.output.persistence

import com.erp.finance.accounting.application.port.output.DimensionRepository
import com.erp.finance.accounting.domain.model.AccountingDimension
import com.erp.finance.accounting.domain.model.DimensionStatus
import com.erp.finance.accounting.domain.model.DimensionType
import com.erp.finance.accounting.infrastructure.persistence.entity.BaseDimensionEntity
import com.erp.finance.accounting.infrastructure.persistence.entity.BusinessAreaEntity
import com.erp.finance.accounting.infrastructure.persistence.entity.CostCenterEntity
import com.erp.finance.accounting.infrastructure.persistence.entity.DepartmentEntity
import com.erp.finance.accounting.infrastructure.persistence.entity.ProfitCenterEntity
import com.erp.finance.accounting.infrastructure.persistence.entity.ProjectEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaDimensionRepository
    @Inject
    constructor(
        private val entityManager: EntityManager,
    ) : DimensionRepository {
        override fun save(dimension: AccountingDimension): AccountingDimension {
            val entity = findEntity(dimension.type, dimension.id)
            val managed =
                if (entity == null) {
                    val fresh = newEntity(dimension.type)
                    fresh.updateFrom(dimension)
                    entityManager.persist(fresh)
                    fresh
                } else {
                    entity.updateFrom(dimension)
                    entity
                }
            entityManager.flush()
            return managed.toDomain()
        }

        override fun findById(
            type: DimensionType,
            tenantId: UUID,
            id: UUID,
        ): AccountingDimension? =
            entityManager
                .find(entityClass(type), id)
                ?.takeIf { it.tenantId == tenantId }
                ?.toDomain()

        override fun findAll(
            type: DimensionType,
            tenantId: UUID,
            companyCodeId: UUID?,
            status: DimensionStatus?,
        ): List<AccountingDimension> {
            val builder =
                StringBuilder("SELECT d FROM ${entityClass(type).simpleName} d WHERE d.tenantId = :tenantId")
            if (companyCodeId != null) {
                builder.append(" AND d.companyCodeId = :companyCodeId")
            }
            if (status != null) {
                builder.append(" AND d.status = :status")
            }

            val query =
                entityManager
                    .createQuery(builder.toString(), entityClass(type))
                    .setParameter("tenantId", tenantId)
            companyCodeId?.let { query.setParameter("companyCodeId", it) }
            status?.let { query.setParameter("status", it.name) }
            return query.resultList.map(BaseDimensionEntity::toDomain)
        }

        override fun findByIds(
            type: DimensionType,
            tenantId: UUID,
            ids: Set<UUID>,
        ): Map<UUID, AccountingDimension> {
            if (ids.isEmpty()) {
                return emptyMap()
            }
            val results =
                entityManager
                    .createQuery(
                        "SELECT d FROM ${entityClass(type).simpleName} d WHERE d.tenantId = :tenantId AND d.id IN :ids",
                        entityClass(type),
                    ).setParameter("tenantId", tenantId)
                    .setParameter("ids", ids)
                    .resultList
            return results.associate { it.id to it.toDomain() }
        }

        private fun findEntity(
            type: DimensionType,
            id: UUID,
        ): BaseDimensionEntity? = entityManager.find(entityClass(type), id)

        private fun newEntity(type: DimensionType): BaseDimensionEntity =
            when (type) {
                DimensionType.COST_CENTER -> CostCenterEntity()
                DimensionType.PROFIT_CENTER -> ProfitCenterEntity()
                DimensionType.DEPARTMENT -> DepartmentEntity()
                DimensionType.PROJECT -> ProjectEntity()
                DimensionType.BUSINESS_AREA -> BusinessAreaEntity()
            }

        @Suppress("UNCHECKED_CAST")
        private fun entityClass(type: DimensionType): Class<out BaseDimensionEntity> =
            when (type) {
                DimensionType.COST_CENTER -> CostCenterEntity::class.java
                DimensionType.PROFIT_CENTER -> ProfitCenterEntity::class.java
                DimensionType.DEPARTMENT -> DepartmentEntity::class.java
                DimensionType.PROJECT -> ProjectEntity::class.java
                DimensionType.BUSINESS_AREA -> BusinessAreaEntity::class.java
            }
    }
