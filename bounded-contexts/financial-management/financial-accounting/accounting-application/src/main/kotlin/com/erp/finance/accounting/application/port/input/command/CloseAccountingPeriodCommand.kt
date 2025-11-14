package com.erp.finance.accounting.application.port.input.command

import java.util.UUID

data class CloseAccountingPeriodCommand(
    val tenantId: UUID,
    val ledgerId: UUID,
    val accountingPeriodId: UUID,
    val freezeOnly: Boolean = false,
)
