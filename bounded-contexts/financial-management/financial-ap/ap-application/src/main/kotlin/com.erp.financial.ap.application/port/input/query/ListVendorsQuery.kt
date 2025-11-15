package com.erp.financial.ap.application.port.input.query

import com.erp.financial.shared.masterdata.MasterDataStatus
import java.util.UUID

data class ListVendorsQuery(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val status: MasterDataStatus? = null,
)
