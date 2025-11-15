package com.erp.finance.accounting.application.port.output

import com.erp.finance.accounting.domain.model.AccountingDimension
import com.erp.finance.accounting.domain.model.DimensionStatus
import com.erp.finance.accounting.domain.model.DimensionType
import java.util.UUID

interface DimensionRepository {
    fun save(dimension: AccountingDimension): AccountingDimension

    fun findById(
        type: DimensionType,
        tenantId: UUID,
        id: UUID,
    ): AccountingDimension?

    fun findAll(
        type: DimensionType,
        tenantId: UUID,
        companyCodeId: UUID? = null,
        status: DimensionStatus? = null,
    ): List<AccountingDimension>

    fun findByIds(
        type: DimensionType,
        tenantId: UUID,
        ids: Set<UUID>,
    ): Map<UUID, AccountingDimension>
}
