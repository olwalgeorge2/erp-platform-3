package com.erp.financial.ap.domain.model.bill

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.shared.Money
import java.util.UUID

data class BillLine(
    val id: UUID = UUID.randomUUID(),
    val glAccountId: UUID,
    val description: String,
    val netAmount: Money,
    val taxAmount: Money = Money.zero(netAmount.currency),
    val dimensionAssignments: DimensionAssignments = DimensionAssignments(),
) {
    init {
        require(description.isNotBlank()) { "Line description cannot be blank" }
        require(glAccountId.variant() == 2) { "glAccountId must be RFC4122 UUID" }
        require(netAmount.currency == taxAmount.currency) { "Currency mismatch for tax amount" }
    }
}
