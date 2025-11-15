package com.erp.financial.ar.application.port.input.command

import com.erp.finance.accounting.domain.model.DimensionAssignments
import java.time.LocalDate
import java.util.UUID

data class CreateCustomerInvoiceCommand(
    val tenantId: UUID,
    val companyCodeId: UUID,
    val customerId: UUID,
    val invoiceNumber: String,
    val invoiceDate: LocalDate,
    val dueDate: LocalDate,
    val currency: String,
    val lines: List<Line>,
    val dimensionAssignments: DimensionAssignments = DimensionAssignments(),
) {
    data class Line(
        val glAccountId: UUID,
        val description: String,
        val netAmount: Long,
        val taxAmount: Long = 0,
        val dimensionAssignments: DimensionAssignments = DimensionAssignments(),
    )
}
