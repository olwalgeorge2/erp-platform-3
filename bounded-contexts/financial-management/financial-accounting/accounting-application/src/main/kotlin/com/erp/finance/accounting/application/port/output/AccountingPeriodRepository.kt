package com.erp.finance.accounting.application.port.output

import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.AccountingPeriodId
import com.erp.finance.accounting.domain.model.LedgerId
import java.util.UUID

interface AccountingPeriodRepository {
    fun save(period: AccountingPeriod): AccountingPeriod

    fun findById(
        id: AccountingPeriodId,
        tenantId: UUID,
    ): AccountingPeriod?

    fun findOpenByLedger(
        ledgerId: LedgerId,
        tenantId: UUID,
    ): List<AccountingPeriod>
}
