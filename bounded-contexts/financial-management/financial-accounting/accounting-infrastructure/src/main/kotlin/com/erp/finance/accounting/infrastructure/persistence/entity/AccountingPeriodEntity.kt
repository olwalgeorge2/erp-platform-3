package com.erp.finance.accounting.infrastructure.persistence.entity

import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.AccountingPeriodId
import com.erp.finance.accounting.domain.model.AccountingPeriodStatus
import com.erp.finance.accounting.domain.model.LedgerId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "accounting_periods", schema = "financial_accounting")
class AccountingPeriodEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "ledger_id", nullable = false)
    var ledgerId: UUID,
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "period_code", nullable = false, length = 32)
    var periodCode: String,
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,
    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: AccountingPeriodStatus = AccountingPeriodStatus.OPEN,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    @Column(name = "version")
    var version: Int? = 0,
) {
    fun toDomain(): AccountingPeriod =
        AccountingPeriod(
            id = AccountingPeriodId(id),
            ledgerId = LedgerId(ledgerId),
            tenantId = tenantId,
            code = periodCode,
            startDate = startDate,
            endDate = endDate,
            status = status,
        )

    companion object {
        fun from(domain: AccountingPeriod): AccountingPeriodEntity =
            AccountingPeriodEntity(
                id = domain.id.value,
                ledgerId = domain.ledgerId.value,
                tenantId = domain.tenantId,
                periodCode = domain.code,
                startDate = domain.startDate,
                endDate = domain.endDate,
                status = domain.status,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
    }
}
