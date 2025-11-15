package com.erp.financial.ap.domain.model.bill

import java.util.UUID

@JvmInline
value class BillId(
    val value: UUID,
) {
    init {
        require(!value.equals(NULL_UUID)) { "BillId cannot be zero UUID" }
    }

    companion object {
        private val NULL_UUID: UUID = UUID(0, 0)

        fun newId(): BillId = BillId(UUID.randomUUID())
    }
}
