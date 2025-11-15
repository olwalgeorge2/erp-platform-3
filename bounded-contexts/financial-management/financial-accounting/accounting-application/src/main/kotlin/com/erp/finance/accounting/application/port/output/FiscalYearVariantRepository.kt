package com.erp.finance.accounting.application.port.output

import com.erp.finance.accounting.domain.model.CompanyCodeFiscalYearVariant
import com.erp.finance.accounting.domain.model.FiscalYearVariant
import com.erp.finance.accounting.domain.model.PeriodBlackout
import java.util.UUID

interface FiscalYearVariantRepository {
    fun save(variant: FiscalYearVariant): FiscalYearVariant

    fun findById(
        tenantId: UUID,
        id: UUID,
    ): FiscalYearVariant?

    fun findByTenant(tenantId: UUID): List<FiscalYearVariant>

    fun assignToCompanyCode(assignment: CompanyCodeFiscalYearVariant): CompanyCodeFiscalYearVariant

    fun scheduleBlackout(blackout: PeriodBlackout): PeriodBlackout
}
