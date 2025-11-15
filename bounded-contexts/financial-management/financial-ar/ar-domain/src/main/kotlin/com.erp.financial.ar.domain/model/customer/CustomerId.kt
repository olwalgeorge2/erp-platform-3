package com.erp.financial.ar.domain.model.customer

import java.util.UUID

@JvmInline
value class CustomerId(
    val value: UUID,
) {
    init {
        require(value.variant() == 2) { "CustomerId must be RFC4122 UUID" }
    }

    companion object {
        fun newId(): CustomerId = CustomerId(UUID.randomUUID())
    }
}
