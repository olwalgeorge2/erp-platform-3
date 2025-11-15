package com.erp.finance.accounting.domain.model

import java.time.Instant
import java.util.UUID

data class CompanyCode(
    val id: UUID = UUID.randomUUID(),
    val tenantId: UUID,
    val code: String,
    val name: String,
    val legalEntityName: String,
    val countryCode: String,
    val baseCurrency: String,
    val timezone: String,
    val status: String = "ACTIVE",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    init {
        require(code.isNotBlank()) { "Company code cannot be blank" }
        require(name.isNotBlank()) { "Company name cannot be blank" }
        require(legalEntityName.isNotBlank()) { "Legal entity name cannot be blank" }
        require(countryCode.length == 2) { "Country code must be ISO-3166 alpha-2" }
        require(baseCurrency.length == 3) { "Base currency must be ISO-4217 3-letter code" }
        require(timezone.isNotBlank()) { "Timezone cannot be blank" }
    }
}
