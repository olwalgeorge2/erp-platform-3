package com.erp.finance.accounting.application.port.input.command

import com.erp.finance.accounting.domain.model.AccountType
import java.util.UUID

data class DefineAccountCommand(
    val tenantId: UUID,
    val chartOfAccountsId: UUID,
    val parentAccountId: UUID? = null,
    val code: String,
    val name: String,
    val type: AccountType,
    val currency: String? = null,
    val isPosting: Boolean = true,
)
