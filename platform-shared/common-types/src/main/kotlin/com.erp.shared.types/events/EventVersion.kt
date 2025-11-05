package com.erp.shared.types.events

@JvmInline
value class EventVersion private constructor(
    val value: Int,
) {
    init {
        require(value > 0) { "Event version must be positive" }
    }

    companion object {
        fun initial(): EventVersion = EventVersion(1)

        fun of(version: Int): EventVersion = EventVersion(version)
    }

    fun next(): EventVersion = EventVersion(value + 1)
}
