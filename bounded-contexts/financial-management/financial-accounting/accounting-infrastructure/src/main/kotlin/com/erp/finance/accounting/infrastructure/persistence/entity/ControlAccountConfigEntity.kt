package com.erp.finance.accounting.infrastructure.persistence.entity

import com.erp.finance.accounting.domain.model.ControlAccountCategory
import com.erp.finance.accounting.domain.model.ControlAccountConfig
import com.erp.finance.accounting.domain.model.ControlAccountSubLedger
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "control_account_config",
    schema = "financial_accounting",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_control_account_config_key",
            columnNames = [
                "tenant_id",
                "company_code_id",
                "subledger",
                "category",
                "dimension_key",
                "currency",
            ],
        ),
    ],
)
class ControlAccountConfigEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "company_code_id", nullable = false)
    var companyCodeId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "subledger", nullable = false, length = 8)
    var subLedger: ControlAccountSubLedger,
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 16)
    var category: ControlAccountCategory,
    @Column(name = "dimension_key", nullable = false, length = 128)
    var dimensionKey: String,
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,
    @Column(name = "gl_account_id", nullable = false)
    var glAccountId: UUID,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    fun toDomain(): ControlAccountConfig =
        ControlAccountConfig(
            id = id,
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            subLedger = subLedger,
            category = category,
            dimensionKey = dimensionKey,
            currency = currency,
            glAccountId = glAccountId,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun from(domain: ControlAccountConfig): ControlAccountConfigEntity =
            ControlAccountConfigEntity(
                id = domain.id,
                tenantId = domain.tenantId,
                companyCodeId = domain.companyCodeId,
                subLedger = domain.subLedger,
                category = domain.category,
                dimensionKey = domain.dimensionKey,
                currency = domain.currency,
                glAccountId = domain.glAccountId,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
    }
}
