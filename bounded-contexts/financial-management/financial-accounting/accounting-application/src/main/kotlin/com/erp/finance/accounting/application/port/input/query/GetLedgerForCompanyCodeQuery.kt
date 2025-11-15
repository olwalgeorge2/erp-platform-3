package com.erp.finance.accounting.application.port.input.query

import java.util.UUID

data class GetLedgerForCompanyCodeQuery(
    val companyCodeId: UUID,
    val tenantId: UUID,
)
