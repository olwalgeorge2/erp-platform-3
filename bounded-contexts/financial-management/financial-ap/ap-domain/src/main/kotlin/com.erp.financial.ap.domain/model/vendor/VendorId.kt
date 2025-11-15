package com.erp.financial.ap.domain.model.vendor

import java.util.UUID

@JvmInline
value class VendorId(
    val value: UUID,
) {
    init {
        require(value.version() == 4 || value.version() == 1) { "VendorId must be a UUID" }
    }

    companion object {
        fun newId(): VendorId = VendorId(UUID.randomUUID())
    }
}
