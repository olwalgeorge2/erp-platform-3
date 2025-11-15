package com.erp.finance.accounting.infrastructure.persistence.entity

import com.erp.finance.accounting.domain.model.CompanyCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "company_codes", schema = "financial_accounting")
class CompanyCodeEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "code", nullable = false, length = 32)
    var code: String,
    @Column(name = "name", nullable = false, length = 255)
    var name: String,
    @Column(name = "legal_entity_name", nullable = false, length = 255)
    var legalEntityName: String,
    @Column(name = "country_code", nullable = false, length = 2)
    var countryCode: String,
    @Column(name = "base_currency", nullable = false, length = 3)
    var baseCurrency: String,
    @Column(name = "timezone", nullable = false, length = 64)
    var timezone: String,
    @Column(name = "status", nullable = false, length = 32)
    var status: String = "ACTIVE",
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    fun toDomain(): CompanyCode =
        CompanyCode(
            id = id,
            tenantId = tenantId,
            code = code,
            name = name,
            legalEntityName = legalEntityName,
            countryCode = countryCode,
            baseCurrency = baseCurrency,
            timezone = timezone,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    fun updateFrom(domain: CompanyCode) {
        id = domain.id
        tenantId = domain.tenantId
        code = domain.code
        name = domain.name
        legalEntityName = domain.legalEntityName
        countryCode = domain.countryCode
        baseCurrency = domain.baseCurrency
        timezone = domain.timezone
        status = domain.status
        createdAt = domain.createdAt
        updatedAt = domain.updatedAt
    }

    companion object {
        fun from(domain: CompanyCode): CompanyCodeEntity =
            CompanyCodeEntity(
                id = domain.id,
                tenantId = domain.tenantId,
                code = domain.code,
                name = domain.name,
                legalEntityName = domain.legalEntityName,
                countryCode = domain.countryCode,
                baseCurrency = domain.baseCurrency,
                timezone = domain.timezone,
                status = domain.status,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
    }
}
