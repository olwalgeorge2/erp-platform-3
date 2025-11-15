package com.erp.finance.accounting.application.port.output

import com.erp.finance.accounting.domain.model.CompanyCode
import java.util.UUID

interface CompanyCodeRepository {
    fun save(companyCode: CompanyCode): CompanyCode

    fun findById(
        tenantId: UUID,
        id: UUID,
    ): CompanyCode?

    fun findByTenant(tenantId: UUID): List<CompanyCode>
}
