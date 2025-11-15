package com.erp.finance.accounting.domain.model

import java.util.UUID

data class DimensionAssignments(
    val costCenterId: UUID? = null,
    val profitCenterId: UUID? = null,
    val departmentId: UUID? = null,
    val projectId: UUID? = null,
    val businessAreaId: UUID? = null,
) {
    fun isEmpty(): Boolean =
        costCenterId == null &&
            profitCenterId == null &&
            departmentId == null &&
            projectId == null &&
            businessAreaId == null
}
