# Kafka Integration Manual Verification Steps

> **⚠️ DEPRECATED DOCUMENT**: This document describes verification steps for Apache Kafka. The platform has been migrated to **Redpanda**. See [`REDPANDA_MIGRATION.md`](REDPANDA_MIGRATION.md) for current instructions.
>
> For Redpanda verification, use `rpk` CLI: `docker exec -it erp-redpanda rpk topic list`

**Date:** November 6, 2025 (Archived: November 10, 2025)  
**Original Stack:** Apache Kafka 3.8.1 (KRaft mode)  
**Status:** ⚠️ ARCHIVED - Use Redpanda instead

---

## Prerequisites ✅

- ✅ Apache Kafka 3.8.1 running (KRaft mode)
- ✅ PostgreSQL 16 running
- ✅ Kafka UI available at http://localhost:8080
- ✅ Topic consumer ready for `identity.domain.events.v1`

---

## Step 1: Start Kafka Consumer (Already Running)

A consumer is now running in background terminal monitoring:
```bash
docker exec -it erp-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic identity.domain.events.v1 \
  --from-beginning \
  --property print.headers=true \
  --property print.timestamp=true
```

**Status:** ✅ Waiting for events

---

## Step 2: Apply Database Migrations

Before running the application, ensure all Flyway migrations are applied:

```bash
# Check current migration status
./gradlew :bounded-contexts:tenancy-identity:identity-infrastructure:flywayInfo

# Apply migrations
./gradlew :bounded-contexts:tenancy-identity:identity-infrastructure:flywayMigrate
```

**Expected Tables:**
- `identity_tenants`
- `identity_users`
- `identity_roles`
- `identity_outbox_events` ← Critical for outbox pattern

---

## Step 3: Start the Quarkus Application

```bash
./gradlew :bounded-contexts:tenancy-identity:identity-infrastructure:quarkusDev
```

**Wait for:**
- `Installed features: [...]` message
- `Listening on: http://localhost:8080` (or configured port)
- Check outbox scheduler starts: `OutboxEventScheduler` logs

---

## Step 4: Hit User Creation API

Create a user with correlation ID:

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -H "X-Trace-ID: test-trace-123" \
  -H "X-Tenant-ID: tenant-001" \
  -d '{
    "username": "john.doe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Expected Response:** `201 Created` with user details

---

## Step 5: Verify Kafka Events

### 5.1 Check Consumer Output

The running consumer should show:
```
CreateTimestamp:1730908800000, traceId:test-trace-123, tenantId:tenant-001, eventType:UserCreated
{"eventId":"...","aggregateId":"...","eventType":"UserCreated","payload":{...},"occurredAt":"..."}
```

**Verify:**
- ✅ Headers present: `traceId`, `tenantId`, `eventType`
- ✅ Payload contains user data
- ✅ Event published within ~5 seconds (scheduler interval)

### 5.2 Check Kafka UI

Open http://localhost:8080 and navigate to:
1. Topics → `identity.domain.events.v1`
2. Messages → View last messages
3. Verify headers are visible in message details

---

## Step 6: Inspect Prometheus Metrics

```bash
curl http://localhost:8080/q/metrics | grep identity.outbox
```

**Expected Metrics:**
```
# HELP identity_outbox_published_total Total number of outbox events published
# TYPE identity_outbox_published_total counter
identity_outbox_published_total{class="...KafkaOutboxMessagePublisher"} 1.0

# HELP identity_outbox_publish_duration_seconds Time taken to publish outbox events
# TYPE identity_outbox_publish_duration_seconds summary
identity_outbox_publish_duration_seconds_count{class="...KafkaOutboxMessagePublisher"} 1.0
identity_outbox_publish_duration_seconds_sum{class="...KafkaOutboxMessagePublisher"} 0.245
```

**Verify:**
- ✅ `identity_outbox_published_total` increments
- ✅ `identity_outbox_publish_duration_seconds` shows timing

---

## Step 7: Verify Database Outbox Status

```bash
docker exec -it erp-postgres psql -U erp_user -d erp_identity -c "
SELECT 
  id, 
  event_type, 
  status, 
  published_at, 
  recorded_at,
  recorded_at - published_at as publish_delay
FROM identity_outbox_events 
ORDER BY recorded_at DESC 
LIMIT 5;
"
```

**Expected:**
- Status: `PUBLISHED`
- `published_at` timestamp populated
- `publish_delay` should be ~5 seconds (scheduler interval)

---

## Step 8: Test Failure Scenarios

### 8.1 Stop Kafka
```bash
docker stop erp-kafka
```

### 8.2 Create Another User
Same curl command as Step 4

**Expected Behavior:**
- User created successfully (DB transaction succeeds)
- Event saved to outbox with status `PENDING`
- Application logs show Kafka publish failure
- Outbox `failure_count` increments

### 8.3 Restart Kafka
```bash
docker start erp-kafka
```

**Expected:**
- Within 5 seconds, scheduler retries pending events
- Event status changes to `PUBLISHED`
- Kafka consumer receives the delayed event

---

## Success Criteria ✅

- [ ] Events published to Kafka within 5 seconds
- [ ] Headers (traceId, tenantId, eventType) present in Kafka messages
- [ ] Outbox rows transition from `PENDING` → `PUBLISHED`
- [ ] `published_at` timestamp populated
- [ ] Prometheus metrics incrementing correctly
- [ ] Failure resilience: events retry after Kafka restart
- [ ] No duplicate events (idempotency maintained)

---

## Troubleshooting

### Consumer Not Receiving Events
```bash
# Check topic exists
docker exec erp-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list

# Check consumer group
docker exec erp-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --list
```

### Outbox Events Stuck in PENDING
```bash
# Check scheduler is running
docker logs <quarkus-container> | grep OutboxEventScheduler

# Check Kafka connectivity from application
curl http://localhost:8080/q/health
```

### Database Connection Issues
```bash
# Verify Postgres is accessible
docker exec -it erp-postgres psql -U erp_user -d erp_identity -c "\dt"
```

---

## Next Steps After Verification

1. **Create Database Indexes** (V003 migration)
   - `idx_users_tenant_status`
   - `idx_users_tenant_created_at`
   - `idx_roles_tenant`
   - `idx_outbox_pending_events`
   - `idx_outbox_failed_events`

2. **Expand Unit Test Coverage**
   - PasswordPolicyTest
   - UserTest (aggregate behavior)
   - TenantTest
   - AuthenticationServiceTest
   - JpaUserRepositoryTest (with Testcontainers)
   - JpaTenantRepositoryTest
   - JpaRoleRepositoryTest
   - OutboxEventSchedulerTest

3. **Integration Tests**
   - UserCommandHandlerIntegrationTest (end-to-end)
   - Kafka event publishing with Testcontainers
   - Transaction rollback scenarios
   - Multi-tenant isolation verification

**Estimated Remaining Work:** 8-11 hours to Phase 2.1 completion

---

**Last Updated:** November 6, 2025  
**Verification Status:** 🟡 Ready for manual testing
