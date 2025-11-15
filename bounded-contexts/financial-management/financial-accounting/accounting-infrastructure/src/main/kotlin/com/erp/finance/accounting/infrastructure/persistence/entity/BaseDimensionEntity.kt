package com.erp.finance.accounting.infrastructure.persistence.entity

import com.erp.finance.accounting.domain.model.AccountingDimension
import com.erp.finance.accounting.domain.model.DimensionStatus
import com.erp.finance.accounting.domain.model.DimensionType
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@MappedSuperclass
abstract class BaseDimensionEntity {
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID()

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID = UUID.randomUUID()

    @Column(name = "company_code_id", nullable = false)
    var companyCodeId: UUID = UUID.randomUUID()

    @Column(name = "code", nullable = false, length = 64)
    var code: String = ""

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "description")
    var description: String? = null

    @Column(name = "parent_id")
    var parentId: UUID? = null

    @Column(name = "status", nullable = false, length = 32)
    var status: String = DimensionStatus.DRAFT.name

    @Column(name = "valid_from", nullable = false)
    var validFrom: LocalDate = LocalDate.now()

    @Column(name = "valid_to")
    var validTo: LocalDate? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    abstract val type: DimensionType

    fun toDomain(): AccountingDimension =
        AccountingDimension(
            id = id,
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            type = type,
            code = code,
            name = name,
            description = description,
            parentId = parentId,
            status = DimensionStatus.valueOf(status),
            validFrom = validFrom,
            validTo = validTo,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    open fun updateFrom(dimension: AccountingDimension) {
        id = dimension.id
        tenantId = dimension.tenantId
        companyCodeId = dimension.companyCodeId
        code = dimension.code
        name = dimension.name
        description = dimension.description
        parentId = dimension.parentId
        status = dimension.status.name
        validFrom = dimension.validFrom
        validTo = dimension.validTo
        createdAt = dimension.createdAt
        updatedAt = dimension.updatedAt
    }
}
