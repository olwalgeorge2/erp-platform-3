package com.erp.finance.accounting.infrastructure.persistence.entity

import com.erp.finance.accounting.domain.model.ChartOfAccountsId
import com.erp.finance.accounting.domain.model.Ledger
import com.erp.finance.accounting.domain.model.LedgerId
import com.erp.finance.accounting.domain.model.LedgerStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ledgers", schema = "financial_accounting")
class LedgerEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "chart_of_accounts_id", nullable = false)
    var chartOfAccountsId: UUID,
    @Column(name = "base_currency", nullable = false, length = 3)
    var baseCurrency: String,
    @Column(name = "status", nullable = false, length = 32)
    var status: String = LedgerStatus.ACTIVE.name,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    @Column(name = "version")
    var version: Int? = 0,
) {
    fun toDomain(): Ledger =
        Ledger(
            id = LedgerId(id),
            tenantId = tenantId,
            chartOfAccountsId = ChartOfAccountsId(chartOfAccountsId),
            baseCurrency = baseCurrency,
            status = LedgerStatus.valueOf(status),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun from(domain: Ledger): LedgerEntity =
            LedgerEntity(
                id = domain.id.value,
                tenantId = domain.tenantId,
                chartOfAccountsId = domain.chartOfAccountsId.value,
                baseCurrency = domain.baseCurrency,
                status = domain.status.name,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
    }
}
