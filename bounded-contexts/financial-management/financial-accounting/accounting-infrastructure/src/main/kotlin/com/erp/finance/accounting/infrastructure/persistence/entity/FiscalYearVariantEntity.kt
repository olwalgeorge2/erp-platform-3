package com.erp.finance.accounting.infrastructure.persistence.entity

import com.erp.finance.accounting.domain.model.CompanyCodeFiscalYearVariant
import com.erp.finance.accounting.domain.model.FiscalYearVariant
import com.erp.finance.accounting.domain.model.FiscalYearVariantPeriod
import com.erp.finance.accounting.domain.model.PeriodBlackout
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "fiscal_year_variants", schema = "financial_accounting")
class FiscalYearVariantEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "code", nullable = false, length = 64)
    var code: String,
    @Column(name = "name", nullable = false, length = 255)
    var name: String,
    @Column(name = "description")
    var description: String? = null,
    @Column(name = "start_month", nullable = false)
    var startMonth: Int = 1,
    @Column(name = "calendar_pattern", nullable = false, length = 32)
    var calendarPattern: String = "CALENDAR",
    @OneToMany(mappedBy = "variant", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var periods: MutableList<FiscalYearVariantPeriodEntity> = mutableListOf(),
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    fun toDomain(): FiscalYearVariant =
        FiscalYearVariant(
            id = id,
            tenantId = tenantId,
            code = code,
            name = name,
            description = description,
            startMonth = startMonth,
            calendarPattern = calendarPattern,
            periods = periods.sortedBy { it.periodNumber }.map(FiscalYearVariantPeriodEntity::toDomain),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    fun updateFrom(domain: FiscalYearVariant) {
        id = domain.id
        tenantId = domain.tenantId
        code = domain.code
        name = domain.name
        description = domain.description
        startMonth = domain.startMonth
        calendarPattern = domain.calendarPattern
        createdAt = domain.createdAt
        updatedAt = domain.updatedAt

        periods.clear()
        domain.periods.forEach {
            periods +=
                FiscalYearVariantPeriodEntity(
                    variant = this,
                    periodNumber = it.periodNumber,
                    label = it.label,
                    lengthInDays = it.lengthInDays,
                    isAdjustment = it.adjustment,
                )
        }
    }

    companion object {
        fun from(domain: FiscalYearVariant): FiscalYearVariantEntity =
            FiscalYearVariantEntity(
                id = domain.id,
                tenantId = domain.tenantId,
                code = domain.code,
                name = domain.name,
                description = domain.description,
                startMonth = domain.startMonth,
                calendarPattern = domain.calendarPattern,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            ).also { entity -> entity.updateFrom(domain) }
    }
}

@Entity
@Table(name = "fiscal_year_variant_periods", schema = "financial_accounting")
class FiscalYearVariantPeriodEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    var variant: FiscalYearVariantEntity,
    @Column(name = "period_number", nullable = false)
    var periodNumber: Int,
    @Column(name = "label", nullable = false, length = 64)
    var label: String,
    @Column(name = "length_in_days", nullable = false)
    var lengthInDays: Int,
    @Column(name = "is_adjustment", nullable = false)
    var isAdjustment: Boolean = false,
) {
    fun toDomain(): FiscalYearVariantPeriod =
        FiscalYearVariantPeriod(
            periodNumber = periodNumber,
            label = label,
            lengthInDays = lengthInDays,
            adjustment = isAdjustment,
        )
}

@Entity
@Table(name = "company_code_fiscal_year_variants", schema = "financial_accounting")
class CompanyCodeFiscalYearVariantEntity(
    @EmbeddedId
    var id: CompanyCodeFiscalYearVariantKey = CompanyCodeFiscalYearVariantKey(),
    @Column(name = "effective_from", nullable = false)
    var effectiveFrom: Instant = Instant.now(),
    @Column(name = "effective_to")
    var effectiveTo: Instant? = null,
) {
    fun toDomain(): CompanyCodeFiscalYearVariant =
        CompanyCodeFiscalYearVariant(
            companyCodeId = id.companyCodeId,
            fiscalYearVariantId = id.variantId,
            effectiveFrom = effectiveFrom,
            effectiveTo = effectiveTo,
        )

    companion object {
        fun from(domain: CompanyCodeFiscalYearVariant): CompanyCodeFiscalYearVariantEntity =
            CompanyCodeFiscalYearVariantEntity(
                id =
                    CompanyCodeFiscalYearVariantKey(
                        companyCodeId = domain.companyCodeId,
                        variantId = domain.fiscalYearVariantId,
                    ),
                effectiveFrom = domain.effectiveFrom,
                effectiveTo = domain.effectiveTo,
            )
    }
}

@Embeddable
data class CompanyCodeFiscalYearVariantKey(
    @Column(name = "company_code_id", nullable = false)
    var companyCodeId: UUID = UUID.randomUUID(),
    @Column(name = "fiscal_year_variant_id", nullable = false)
    var variantId: UUID = UUID.randomUUID(),
)

@Entity
@Table(name = "period_blackouts", schema = "financial_accounting")
class PeriodBlackoutEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "company_code_id", nullable = false)
    var companyCodeId: UUID,
    @Column(name = "period_code", nullable = false, length = 32)
    var periodCode: String,
    @Column(name = "blackout_start", nullable = false)
    var blackoutStart: Instant,
    @Column(name = "blackout_end", nullable = false)
    var blackoutEnd: Instant,
    @Column(name = "status", nullable = false, length = 32)
    var status: String = "PLANNED",
    @Column(name = "reason")
    var reason: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    fun toDomain(): PeriodBlackout =
        PeriodBlackout(
            id = id,
            companyCodeId = companyCodeId,
            periodCode = periodCode,
            blackoutStart = blackoutStart,
            blackoutEnd = blackoutEnd,
            status = status,
            reason = reason,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    fun updateFrom(domain: PeriodBlackout) {
        id = domain.id
        companyCodeId = domain.companyCodeId
        periodCode = domain.periodCode
        blackoutStart = domain.blackoutStart
        blackoutEnd = domain.blackoutEnd
        status = domain.status
        reason = domain.reason
        createdAt = domain.createdAt
        updatedAt = domain.updatedAt
    }

    companion object {
        fun from(domain: PeriodBlackout): PeriodBlackoutEntity =
            PeriodBlackoutEntity(
                id = domain.id,
                companyCodeId = domain.companyCodeId,
                periodCode = domain.periodCode,
                blackoutStart = domain.blackoutStart,
                blackoutEnd = domain.blackoutEnd,
                status = domain.status,
                reason = domain.reason,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
    }
}
