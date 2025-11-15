package com.erp.finance.accounting.application.port.output

import java.util.UUID

interface CompanyCodeLedgerRepository {
    fun linkLedger(
        companyCodeId: UUID,
        ledgerId: UUID,
    )

    fun findLedgersForCompanyCode(companyCodeId: UUID): List<UUID>
}
