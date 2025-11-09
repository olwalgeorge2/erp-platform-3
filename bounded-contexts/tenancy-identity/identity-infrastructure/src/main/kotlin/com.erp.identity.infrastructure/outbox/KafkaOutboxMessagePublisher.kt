package com.erp.identity.infrastructure.outbox

import com.erp.shared.types.results.Result
import io.micrometer.core.annotation.Counted
import io.micrometer.core.annotation.Timed
import io.quarkus.logging.Log
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import org.jboss.logging.MDC
import java.util.UUID

@ApplicationScoped
class KafkaOutboxMessagePublisher(
    @param:Channel("identity-events-out")
    private val emitter: Emitter<String>,
) : OutboxMessagePublisher {
    @Counted(
        value = "identity.outbox.publish.attempts",
        description = "Total outbox event publish attempts",
    )
    @Timed(
        value = "identity.outbox.publish.duration",
        description = "Duration of outbox event publish operation",
        percentiles = [0.5, 0.95, 0.99],
    )
    override fun publish(
        eventType: String,
        aggregateId: String?,
        payload: String,
        version: Int,
    ): Result<Unit> {
        val traceId = MDC.get("traceId")?.toString() ?: UUID.randomUUID().toString()

        return try {
            // Generate message key for partition routing (use aggregateId for ordering)
            val messageKey = aggregateId ?: UUID.randomUUID().toString()

            // Build headers list
            val headers = mutableListOf<org.apache.kafka.common.header.internals.RecordHeader>()
            headers.add(
                org.apache.kafka.common.header.internals
                    .RecordHeader("event-type", eventType.toByteArray()),
            )
            headers.add(
                org.apache.kafka.common.header.internals
                    .RecordHeader("trace-id", traceId.toByteArray()),
            )

            // Add tenant-id if available in MDC
            MDC.get("tenantId")?.toString()?.let { tenantId ->
                headers.add(
                    org.apache.kafka.common.header.internals
                        .RecordHeader("tenant-id", tenantId.toByteArray()),
                )
            }

            // Add aggregate-id if present
            aggregateId?.let { aggId ->
                headers.add(
                    org.apache.kafka.common.header.internals
                        .RecordHeader("aggregate-id", aggId.toByteArray()),
                )
            }

            // Add event-version header from DomainEvent.version
            headers.add(
                org.apache.kafka.common.header.internals
                    .RecordHeader("event-version", version.toString().toByteArray()),
            )

            // Create Kafka metadata with headers
            val metadata =
                OutgoingKafkaRecordMetadata
                    .builder<String>()
                    .withKey(messageKey)
                    .withHeaders(headers)
                    .build()

            // Send message with metadata
            val message =
                Message
                    .of(payload)
                    .addMetadata(metadata)
                    .withAck {
                        Log.debugf(
                            "[%s] Kafka ACK received for event-type=%s, key=%s",
                            traceId,
                            eventType,
                            messageKey,
                        )
                        java.util.concurrent.CompletableFuture
                            .completedFuture(null)
                    }.withNack { throwable ->
                        Log.errorf(
                            throwable,
                            "[%s] Kafka NACK received for event-type=%s, key=%s",
                            traceId,
                            eventType,
                            messageKey,
                        )
                        java.util.concurrent.CompletableFuture
                            .completedFuture(null)
                    }

            emitter.send(message)

            Log.debugf(
                "[%s] Published to Kafka: event-type=%s, key=%s, payload-size=%d bytes",
                traceId,
                eventType,
                messageKey,
                payload.length,
            )

            Result.success(Unit)
        } catch (ex: Exception) {
            Log.errorf(
                ex,
                "[%s] Failed to publish to Kafka: event-type=%s",
                traceId,
                eventType,
            )
            Result.failure(
                code = "KAFKA_PUBLISH_FAILED",
                message = "Failed to publish event to Kafka",
                details =
                    mapOf(
                        "eventType" to eventType,
                        "aggregateId" to (aggregateId ?: "n/a"),
                    ),
                cause = ex,
            )
        }
    }
}
