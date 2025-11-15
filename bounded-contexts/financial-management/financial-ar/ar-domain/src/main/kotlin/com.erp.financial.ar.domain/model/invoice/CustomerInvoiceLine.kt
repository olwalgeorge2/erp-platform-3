package com.erp.financial.ar.domain.model.invoice

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.shared.Money
import java.util.UUID

data class CustomerInvoiceLine(
    val id: UUID = UUID.randomUUID(),
    val glAccountId: UUID,
    val description: String,
    val netAmount: Money,
    val taxAmount: Money = Money.zero(netAmount.currency),
    val dimensionAssignments: DimensionAssignments = DimensionAssignments(),
) {
    init {
        require(description.isNotBlank()) { "Line description cannot be blank" }
        require(glAccountId.variant() == 2) { "Account id must be RFC4122 UUID" }
    }
}
