package com.erp.finance.accounting.application.port.output

import com.erp.finance.accounting.domain.model.ControlAccountCategory
import com.erp.finance.accounting.domain.model.ControlAccountConfig
import com.erp.finance.accounting.domain.model.ControlAccountSubLedger
import java.util.UUID

interface ControlAccountRepository {
    fun save(config: ControlAccountConfig): ControlAccountConfig

    fun findAccount(
        tenantId: UUID,
        companyCodeId: UUID,
        subLedger: ControlAccountSubLedger,
        category: ControlAccountCategory,
        dimensionKey: String,
        currency: String,
    ): ControlAccountConfig?
}
