package com.erp.identity.infrastructure.consumer

import io.quarkus.logging.Log
import io.smallrye.mutiny.Uni
import io.smallrye.reactive.messaging.annotations.Blocking
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@ApplicationScoped
class IdentityEventConsumer(
    private val processedRepo: ProcessedEventRepository,
) {
    @Incoming("identity-events-in")
    @Blocking
    @Transactional
    fun onEvent(message: Message<String>): Uni<Void> {
        val metadata = message.getMetadata(IncomingKafkaRecordMetadata::class.java).orElse(null)
        val headers = metadata?.headers

        val eventType =
            headers
                ?.lastHeader("event-type")
                ?.value()
                ?.let { String(it, StandardCharsets.UTF_8) }
                ?: "unknown"

        val aggregateId =
            headers
                ?.lastHeader("aggregate-id")
                ?.value()
                ?.let { String(it, StandardCharsets.UTF_8) }

        val version =
            headers
                ?.lastHeader("event-version")
                ?.value()
                ?.let { String(it, StandardCharsets.UTF_8) }
                ?: "1"

        val payload = message.payload
        val fingerprint = fingerprint("$eventType|$version|${aggregateId ?: "n/a"}|$payload")

        return Uni
            .createFrom()
            .item {
                if (processedRepo.alreadyProcessed(fingerprint)) {
                    Log.debugf(
                        "Skip duplicate event: type=%s, version=%s, aggregateId=%s",
                        eventType,
                        version,
                        aggregateId,
                    )
                } else {
                    // Place holder for idempotent handling (update read models, projections, etc.)
                    Log.debugf(
                        "Consume event: type=%s, version=%s, aggregateId=%s, payloadSize=%d",
                        eventType,
                        version,
                        aggregateId,
                        payload.length,
                    )
                    processedRepo.markProcessed(fingerprint)
                }
            }.replaceWithVoid()
            .onFailure()
            .invoke { ex ->
                // Let the framework route to DLQ when configured
                Log.errorf(ex, "Failed to process event: type=%s", eventType)
            }
    }

    private fun fingerprint(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b) }
    }
}
