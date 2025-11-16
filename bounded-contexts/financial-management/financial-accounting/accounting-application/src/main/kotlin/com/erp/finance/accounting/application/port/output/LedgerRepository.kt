package com.erp.finance.accounting.application.port.output

import com.erp.finance.accounting.domain.model.Ledger
import com.erp.finance.accounting.domain.model.LedgerId
import java.util.UUID

interface LedgerRepository {
    fun save(ledger: Ledger): Ledger

    fun findById(
        id: LedgerId,
        tenantId: UUID,
    ): Ledger?

    fun findRecent(
        limit: Int,
    ): List<Ledger>
}
