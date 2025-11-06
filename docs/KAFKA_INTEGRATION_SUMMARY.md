# Kafka Integration Summary

## ✅ Files Created/Modified

### 1. **KafkaOutboxMessagePublisher.kt** (NEW)
- Location: `identity-infrastructure/src/main/kotlin/.../outbox/KafkaOutboxMessagePublisher.kt`
- Implements: `OutboxMessagePublisher` interface
- Features:
  - Kafka message publishing with headers
  - Partition routing by `aggregateId` (ensures event ordering per aggregate)
  - Metrics: `@Counted` and `@Timed` on publish attempts
  - Headers: `event-type`, `trace-id`, `tenant-id`, `aggregate-id`
  - ACK/NACK callbacks for observability

### 2. **OutboxEventScheduler.kt** (UPDATED)
- Added: `MeterRegistry` injection
- Added metrics:
  - `identity.outbox.events.published{outcome=success}`
  - `identity.outbox.events.published{outcome=failure}`
  - `identity.outbox.batch.duration`
- Added: Batch summary logging (success/failure counts)

### 3. **build.gradle.kts** (UPDATED)
- Added dependency: `io.quarkus:quarkus-messaging-kafka`

### 4. **application.properties** (UPDATED)
- Kafka bootstrap servers: `localhost:9092`
- Outgoing channel: `identity-events-out`
- Topic: `identity.domain.events.v1`
- Producer settings:
  - `acks=all` (strongest durability)
  - `retries=3`
  - `enable-idempotence=true`
  - `compression-type=snappy`

---

## 🔧 How It Works

### Event Flow:
```
Domain Event → EventPublisherPort → OutboxEventEntity (DB)
                                           ↓
                       OutboxEventScheduler (@Scheduled 5s)
                                           ↓
                       KafkaOutboxMessagePublisher
                                           ↓
                       Kafka Topic: identity.domain.events.v1
```

### CDI Wiring (Automatic):
Quarkus will automatically inject `KafkaOutboxMessagePublisher` into `OutboxEventScheduler` because:
1. `KafkaOutboxMessagePublisher` is `@ApplicationScoped`
2. It implements `OutboxMessagePublisher` interface
3. `OutboxEventScheduler` declares dependency on `OutboxMessagePublisher`

**No manual wiring needed!** 🎉

---

## 🧪 Testing Locally

### Start Kafka with Docker:

```bash
# Using docker-compose (recommended)
cat > docker-compose-kafka.yml <<EOF
version: '3'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
EOF

docker-compose -f docker-compose-kafka.yml up -d
```

### Verify Kafka is Running:

```bash
# Check logs
docker logs -f $(docker ps -q --filter ancestor=confluentinc/cp-kafka)

# List topics (should auto-create identity.domain.events.v1)
docker exec -it $(docker ps -q --filter ancestor=confluentinc/cp-kafka) \
  kafka-topics --bootstrap-server localhost:9092 --list
```

### Consume Events:

```bash
# Terminal 1: Consume events with headers
docker exec -it $(docker ps -q --filter ancestor=confluentinc/cp-kafka) \
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic identity.domain.events.v1 \
  --property print.headers=true \
  --property print.key=true \
  --from-beginning
```

### Generate Test Events:

```bash
# Terminal 2: Create a user (triggers UserCreatedEvent)
curl -X POST http://localhost:8080/api/identity/users \
  -H "Content-Type: application/json" \
  -H "X-Trace-ID: test-trace-123" \
  -H "X-Tenant-ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -d '{
    "tenantId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "password": "SecurePassword123!"
  }'
```

### Expected Output (Consumer Terminal):

```
event-type:UserCreated,trace-id:test-trace-123,tenant-id:a1b2c3d4-...,aggregate-id:9f8e7d6c-...
key:9f8e7d6c-5b4a-3c2d-1e0f-9a8b7c6d5e4f
{
  "eventId": "...",
  "eventType": "UserCreated",
  "occurredAt": "2025-11-06T08:45:00Z",
  "tenantId": "a1b2c3d4-...",
  "userId": "9f8e7d6c-...",
  "username": "john_doe",
  "email": "john@example.com",
  "status": "PENDING"
}
```

---

## 📊 Verify Metrics

```bash
# Check Prometheus metrics
curl http://localhost:8080/metrics | grep identity.outbox

# Expected output:
# identity_outbox_events_published_total{outcome="success",} 5.0
# identity_outbox_events_published_total{outcome="failure",} 0.0
# identity_outbox_batch_duration_seconds_count 12.0
# identity_outbox_batch_duration_seconds_sum 0.456
# identity_outbox_publish_attempts_total 5.0
# identity_outbox_publish_duration_seconds_count 5.0
```

---

## 🔍 Verify Database

```sql
-- Check outbox events are marked as published
SELECT 
  event_id, 
  event_type, 
  status, 
  published_at,
  failure_count
FROM identity_outbox_events
ORDER BY recorded_at DESC
LIMIT 10;

-- Should see status='PUBLISHED' and published_at timestamp
```

---

## 🚨 Troubleshooting

### Issue: "Failed to connect to Kafka"
**Solution:**
- Check Kafka is running: `docker ps | grep kafka`
- Verify port 9092 is exposed
- Check `kafka.bootstrap.servers` in `application.properties`

### Issue: "Topic not found"
**Solution:**
- Kafka auto-creates topics by default
- Check topic exists: `kafka-topics --list`
- Manually create if needed:
  ```bash
  kafka-topics --create \
    --bootstrap-server localhost:9092 \
    --topic identity.domain.events.v1 \
    --partitions 3 \
    --replication-factor 1
  ```

### Issue: "Events stuck in PENDING"
**Solution:**
- Check scheduler logs: Should see "Dispatching X outbox events"
- Check Kafka connection in app logs
- Verify transaction commits (check database)

### Issue: "Duplicate events published"
**Solution:**
- Idempotence is enabled (`enable-idempotence=true`)
- Check `acks=all` is set
- Verify outbox events are marked published

---

## ✅ Success Criteria

Before moving to Argon2id upgrade, verify:

- [ ] Kafka container running on localhost:9092
- [ ] Application starts without errors
- [ ] User creation triggers event in outbox table
- [ ] Scheduler processes outbox every 5s
- [ ] Events appear in Kafka consumer
- [ ] Headers present: `event-type`, `trace-id`, `tenant-id`
- [ ] Metrics available at `/metrics`
- [ ] Database shows `status='PUBLISHED'` and `published_at` timestamp
- [ ] No exceptions in application logs

---

## 📝 Commit Message Template

```bash
git add bounded-contexts/tenancy-identity/identity-infrastructure/
git add docs/PHASE2.1_COMPLETION_GUIDE.md

git commit -m "feat(identity-outbox): integrate Kafka event publishing

- Replace LoggingOutboxMessagePublisher with KafkaOutboxMessagePublisher
- Add Micrometer metrics to outbox scheduler (published/failed counters)
- Configure Kafka channel with idempotence and strong durability (acks=all)
- Topic: identity.domain.events.v1 with event-type/trace-id headers
- Partition by aggregateId for event ordering guarantees

Metrics added:
- identity.outbox.events.published{outcome=success|failure}
- identity.outbox.batch.duration
- identity.outbox.publish.attempts
- identity.outbox.publish.duration

Kafka config:
- Bootstrap: localhost:9092
- Acks: all
- Retries: 3
- Idempotence: enabled
- Compression: snappy"
```

---

**Status:** ✅ Ready for testing  
**Next:** Argon2id upgrade (see PHASE2.1_COMPLETION_GUIDE.md)
