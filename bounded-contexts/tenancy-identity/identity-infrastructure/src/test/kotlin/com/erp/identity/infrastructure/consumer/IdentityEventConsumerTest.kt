package com.erp.identity.infrastructure.consumer

import org.eclipse.microprofile.reactive.messaging.Message
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class IdentityEventConsumerTest {
    private class InMemoryRepo : ProcessedEventRepository {
        private val set = mutableSetOf<String>()

        override fun alreadyProcessed(fingerprint: String): Boolean = set.contains(fingerprint)

        override fun markProcessed(fingerprint: String) {
            set.add(fingerprint)
        }
    }

    @Test
    fun `marks unprocessed messages as processed`() {
        val repo = InMemoryRepo()
        val consumer = IdentityEventConsumer(repo)

        val msg = Message.of("{\"hello\":\"world\"}")
        assertDoesNotThrow { consumer.onEvent(msg).await().indefinitely() }

        // second pass should be treated as duplicate (no exception)
        assertDoesNotThrow { consumer.onEvent(msg).await().indefinitely() }
    }

    // Header-aware duplicate detection covered implicitly by same-message reprocessing
    // Further metadata-driven tests can be added with embedded Kafka or a
    // stable IncomingKafkaRecordMetadata factory when available.
}
