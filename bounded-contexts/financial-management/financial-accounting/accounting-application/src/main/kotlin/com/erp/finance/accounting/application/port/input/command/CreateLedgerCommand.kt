package com.erp.finance.accounting.application.port.input.command

import java.util.UUID

data class CreateLedgerCommand(
    val tenantId: UUID,
    val chartOfAccountsId: UUID,
    val baseCurrency: String,
    val chartCode: String = "DEFAULT",
    val chartName: String = "Default Chart",
    val reference: String? = null,
)
