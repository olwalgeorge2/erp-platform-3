package com.erp.finance.accounting.application.service

import java.time.Instant
import java.util.UUID

interface DimensionAssignmentValidator {
    fun validateAssignments(
        tenantId: UUID,
        bookedAt: Instant,
        lines: List<DimensionValidationService.DimensionValidationLine>,
    )
}
