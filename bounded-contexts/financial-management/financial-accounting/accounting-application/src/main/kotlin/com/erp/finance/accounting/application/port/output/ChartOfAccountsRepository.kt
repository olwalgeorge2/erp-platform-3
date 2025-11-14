package com.erp.finance.accounting.application.port.output

import com.erp.finance.accounting.domain.model.ChartOfAccounts
import com.erp.finance.accounting.domain.model.ChartOfAccountsId
import java.util.UUID

interface ChartOfAccountsRepository {
    fun save(chartOfAccounts: ChartOfAccounts): ChartOfAccounts

    fun findById(
        id: ChartOfAccountsId,
        tenantId: UUID,
    ): ChartOfAccounts?
}
