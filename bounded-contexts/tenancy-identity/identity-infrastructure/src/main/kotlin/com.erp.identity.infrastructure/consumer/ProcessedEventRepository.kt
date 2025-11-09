package com.erp.identity.infrastructure.consumer

interface ProcessedEventRepository {
    fun alreadyProcessed(fingerprint: String): Boolean

    fun markProcessed(fingerprint: String)
}
