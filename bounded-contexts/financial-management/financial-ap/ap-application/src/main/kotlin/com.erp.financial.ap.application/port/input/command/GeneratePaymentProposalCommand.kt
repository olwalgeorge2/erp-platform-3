package com.erp.financial.ap.application.port.input.command

import java.time.LocalDate
import java.util.UUID

data class GeneratePaymentProposalCommand(
    val tenantId: UUID,
    val companyCodeId: UUID,
    val asOfDate: LocalDate,
    val paymentDate: LocalDate,
    val vendorIds: Set<UUID>? = null,
    val includeDiscountEligible: Boolean = true,
)
