package com.erp.financial.ar.application.port.input.query

import com.erp.financial.shared.masterdata.MasterDataStatus
import java.util.UUID

data class ListCustomersQuery(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val status: MasterDataStatus? = null,
)
