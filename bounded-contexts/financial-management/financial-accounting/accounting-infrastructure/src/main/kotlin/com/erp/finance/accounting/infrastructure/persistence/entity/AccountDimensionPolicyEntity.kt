package com.erp.finance.accounting.infrastructure.persistence.entity

import com.erp.finance.accounting.domain.model.AccountDimensionPolicy
import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.DimensionRequirement
import com.erp.finance.accounting.domain.model.DimensionType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "account_dimension_policies", schema = "financial_accounting")
class AccountDimensionPolicyEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 32)
    var accountType: AccountType,
    @Enumerated(EnumType.STRING)
    @Column(name = "dimension_type", nullable = false, length = 32)
    var dimensionType: DimensionType,
    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_level", nullable = false, length = 16)
    var requirementLevel: DimensionRequirement = DimensionRequirement.OPTIONAL,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    fun toDomain(): AccountDimensionPolicy =
        AccountDimensionPolicy(
            id = id,
            tenantId = tenantId,
            accountType = accountType,
            dimensionType = dimensionType,
            requirement = requirementLevel,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    fun updateFrom(domain: AccountDimensionPolicy) {
        id = domain.id
        tenantId = domain.tenantId
        accountType = domain.accountType
        dimensionType = domain.dimensionType
        requirementLevel = domain.requirement
        createdAt = domain.createdAt
        updatedAt = domain.updatedAt
    }

    companion object {
        fun from(domain: AccountDimensionPolicy): AccountDimensionPolicyEntity =
            AccountDimensionPolicyEntity(
                id = domain.id,
                tenantId = domain.tenantId,
                accountType = domain.accountType,
                dimensionType = domain.dimensionType,
                requirementLevel = domain.requirement,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
    }
}
