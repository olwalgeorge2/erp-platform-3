package com.erp.finance.accounting.application.service

import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.DimensionType
import java.time.LocalDate
import java.util.UUID

class DimensionValidationException(
    val reason: Reason,
    val dimensionType: DimensionType,
    val accountType: AccountType,
    val dimensionId: UUID? = null,
    val bookingDate: LocalDate? = null,
) : RuntimeException(
        "Dimension validation failed: $reason $dimensionType ${dimensionId ?: ""} $accountType",
    ) {
    enum class Reason {
        NOT_FOUND,
        INACTIVE,
        MANDATORY_MISSING,
    }
}
