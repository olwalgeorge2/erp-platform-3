package com.erp.identity.infrastructure.consumer

import com.erp.identity.infrastructure.KafkaTestResource
import com.erp.identity.infrastructure.PostgresTestResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.serialization.StringSerializer
import org.eclipse.microprofile.config.ConfigProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.Properties

@QuarkusTest
@QuarkusTestResource(PostgresTestResource::class)
@QuarkusTestResource(KafkaTestResource::class)
class IdentityEventConsumerKafkaIT {
    @Inject
    lateinit var entityManager: EntityManager

    @Test
    fun `consumer processes unique fingerprints and ignores duplicates`() {
        val topic =
            ConfigProvider
                .getConfig()
                .getValue("mp.messaging.incoming.identity-events-in.topic", String::class.java)
        val bootstrap =
            ConfigProvider
                .getConfig()
                .getValue("mp.messaging.incoming.identity-events-in.bootstrap.servers", String::class.java)

        val props =
            Properties()
                .apply {
                    put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap)
                    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                    put(ProducerConfig.ACKS_CONFIG, "all")
                }

        val producer = KafkaProducer<String, String>(props)

        val before = countProcessed()

        val payload = "{\"hello\":\"world\"}"
        val headers =
            RecordHeaders()
                .add("event-type", "UserRegistered".toByteArray())
                .add("event-version", "1".toByteArray())
                .add("aggregate-id", "11111111-1111-1111-1111-111111111111".toByteArray())

        // Send duplicate messages (same fingerprint)
        send(producer, topic, "key-1", payload, headers)
        send(producer, topic, "key-1", payload, headers)

        // And one distinct message (different version)
        val headersV2 =
            RecordHeaders()
                .add("event-type", "UserRegistered".toByteArray())
                .add("event-version", "2".toByteArray())
                .add("aggregate-id", "11111111-1111-1111-1111-111111111111".toByteArray())
        send(producer, topic, "key-2", payload, headersV2)

        // Await processing
        awaitCount(before + 2, Duration.ofSeconds(10))

        val after = countProcessed()
        assertEquals(before + 2, after, "expected 2 unique processed events")

        producer.flush()
        producer.close()
    }

    @Test
    fun `fingerprint distinguishes same payload with different event types`() {
        val producer = createProducer()
        val topic = getTopic()
        val before = countProcessed()

        val payload = "{\"userId\":\"abc123\"}"
        val aggregateId = "22222222-2222-2222-2222-222222222222"

        // Same payload, version, aggregateId - different event-type
        send(
            producer,
            topic,
            "key-1",
            payload,
            headers("UserCreated", "1", aggregateId),
        )
        send(
            producer,
            topic,
            "key-2",
            payload,
            headers("UserUpdated", "1", aggregateId),
        )

        awaitCount(before + 2, Duration.ofSeconds(10))

        assertEquals(before + 2, countProcessed(), "different event-type should create distinct fingerprints")
        producer.close()
    }

    @Test
    fun `fingerprint distinguishes same payload with different aggregate IDs`() {
        val producer = createProducer()
        val topic = getTopic()
        val before = countProcessed()

        val payload = "{\"action\":\"login\"}"

        // Same event-type, version, payload - different aggregateId
        send(
            producer,
            topic,
            "key-1",
            payload,
            headers("UserLoggedIn", "1", "33333333-3333-3333-3333-333333333333"),
        )
        send(
            producer,
            topic,
            "key-2",
            payload,
            headers("UserLoggedIn", "1", "44444444-4444-4444-4444-444444444444"),
        )

        awaitCount(before + 2, Duration.ofSeconds(10))

        assertEquals(before + 2, countProcessed(), "different aggregate-id should create distinct fingerprints")
        producer.close()
    }

    @Test
    fun `fingerprint distinguishes same payload with different versions`() {
        val producer = createProducer()
        val topic = getTopic()
        val before = countProcessed()

        val payload = "{\"email\":\"user@example.com\"}"
        val aggregateId = "55555555-5555-5555-5555-555555555555"

        // Same event-type, aggregateId, payload - different version
        send(
            producer,
            topic,
            "key-1",
            payload,
            headers("EmailChanged", "1", aggregateId),
        )
        send(
            producer,
            topic,
            "key-2",
            payload,
            headers("EmailChanged", "2", aggregateId),
        )

        awaitCount(before + 2, Duration.ofSeconds(10))

        assertEquals(before + 2, countProcessed(), "different event-version should create distinct fingerprints")
        producer.close()
    }

    @Test
    fun `fingerprint distinguishes different payloads with same headers`() {
        val producer = createProducer()
        val topic = getTopic()
        val before = countProcessed()

        val aggregateId = "66666666-6666-6666-6666-666666666666"
        val commonHeaders = headers("OrderPlaced", "1", aggregateId)

        // Same headers - different payload
        send(producer, topic, "key-1", "{\"orderId\":\"order-1\"}", commonHeaders)
        send(producer, topic, "key-2", "{\"orderId\":\"order-2\"}", commonHeaders)

        awaitCount(before + 2, Duration.ofSeconds(10))

        assertEquals(before + 2, countProcessed(), "different payload should create distinct fingerprints")
        producer.close()
    }

    private fun createProducer(): KafkaProducer<String, String> {
        val bootstrap =
            ConfigProvider
                .getConfig()
                .getValue("mp.messaging.incoming.identity-events-in.bootstrap.servers", String::class.java)

        val props =
            Properties()
                .apply {
                    put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap)
                    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                    put(ProducerConfig.ACKS_CONFIG, "all")
                }

        return KafkaProducer(props)
    }

    private fun getTopic(): String =
        ConfigProvider
            .getConfig()
            .getValue("mp.messaging.incoming.identity-events-in.topic", String::class.java)

    private fun headers(
        eventType: String,
        version: String,
        aggregateId: String,
    ): Headers =
        RecordHeaders()
            .add("event-type", eventType.toByteArray())
            .add("event-version", version.toByteArray())
            .add("aggregate-id", aggregateId.toByteArray())

    private fun send(
        producer: KafkaProducer<String, String>,
        topic: String,
        key: String,
        value: String,
        headers: Headers,
    ) {
        val record = ProducerRecord(topic, null, key, value, headers)
        producer.send(record).get()
    }

    private fun countProcessed(): Long =
        entityManager
            .createQuery("SELECT COUNT(e) FROM ProcessedEventEntity e", java.lang.Long::class.java)
            .singleResult
            .toLong()

    private fun awaitCount(
        expected: Long,
        timeout: Duration,
    ) {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (System.nanoTime() < deadline) {
            if (countProcessed() >= expected) return
            Thread.sleep(100)
        }
    }
}
