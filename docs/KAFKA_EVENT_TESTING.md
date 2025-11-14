# Kafka Event-Driven Architecture - Smoke Test

Test event publishing and consumption across microservices.

## Prerequisites

```powershell
# Start infrastructure
.\scripts\start-infrastructure.ps1

# Start all services
.\scripts\start-all-services.ps1

# Verify Kafka is running
docker ps --filter "name=erp-redpanda"
```

---

## Test 1: List Kafka Topics

```powershell
docker exec erp-redpanda rpk topic list
```

✅ **Expected topics:**
- `identity.tenants`
- `identity.users`
- `identity.roles`
- `finance.ledgers`
- `finance.journal-entries`
- `_schemas` (internal)

---

## Test 2: Monitor Identity Events

### Step 2.1: Start Kafka Consumer (in separate terminal)

```powershell
# Open new PowerShell terminal and run:
docker exec erp-redpanda rpk topic consume identity.tenants --format json
```

Leave this running to see events in real-time.

### Step 2.2: Create a Tenant (trigger event)

```powershell
curl -X POST http://localhost:8081/api/v1/identity/tenants `
  -H "Content-Type: application/json" `
  -d '{
    "name": "Event Test Tenant",
    "slug": "event-test-001",
    "subscription": {
      "plan": "STARTER",
      "startDate": "2025-01-01T00:00:00Z",
      "maxUsers": 10,
      "maxStorage": 1000,
      "features": ["rbac"]
    }
  }'
```

✅ **Expected Kafka Event:**
```json
{
  "value": "{\"eventId\":\"...\",\"tenantId\":\"...\",\"eventType\":\"TenantProvisioned\",\"timestamp\":\"...\",\"data\":{...}}",
  "partition": 0,
  "offset": 0
}
```

### Step 2.3: Activate Tenant (another event)

```powershell
# Replace {TENANT_ID} with ID from step 2.2
curl -X POST http://localhost:8081/api/v1/identity/tenants/{TENANT_ID}/activate `
  -H "Content-Type: application/json" `
  -d '{
    "activatedBy": "admin",
    "reason": "Testing event flow"
  }'
```

✅ **Expected Kafka Event:**
```json
{
  "eventType": "TenantActivated",
  "tenantId": "...",
  "timestamp": "..."
}
```

---

## Test 3: Monitor Finance Events

### Step 3.1: Start Finance Consumer

```powershell
# New terminal
docker exec erp-redpanda rpk topic consume finance.journal-entries --format json
```

### Step 3.2: Post Journal Entry

```powershell
# First create ledger and accounts (see SMOKE_TEST_GUIDE.md for full setup)

curl -X POST http://localhost:8082/api/v1/finance/journal-entries `
  -H "Content-Type: application/json" `
  -H "X-Tenant-Id: {TENANT_ID}" `
  -d '{
    "tenantId": "{TENANT_ID}",
    "ledgerId": "{LEDGER_ID}",
    "accountingPeriodId": "{PERIOD_ID}",
    "reference": "JE-KAFKA-TEST",
    "description": "Testing Kafka event flow",
    "lines": [
      {
        "accountId": "{CASH_ACCOUNT_ID}",
        "direction": "DEBIT",
        "amountMinor": 100000,
        "currency": "USD"
      },
      {
        "accountId": "{REVENUE_ACCOUNT_ID}",
        "direction": "CREDIT",
        "amountMinor": 100000,
        "currency": "USD"
      }
    ]
  }'
```

✅ **Expected Kafka Event:**
```json
{
  "eventType": "JournalEntryPosted",
  "journalEntryId": "...",
  "ledgerId": "...",
  "reference": "JE-KAFKA-TEST",
  "totalDebit": 100000,
  "totalCredit": 100000,
  "timestamp": "..."
}
```

---

## Test 4: Check Historical Messages

```powershell
# View last 10 messages from identity.tenants
docker exec erp-redpanda rpk topic consume identity.tenants `
  --num 10 `
  --offset -10 `
  --format json

# View last 10 messages from finance.journal-entries
docker exec erp-redpanda rpk topic consume finance.journal-entries `
  --num 10 `
  --offset -10 `
  --format json
```

---

## Test 5: Automated Kafka Test Script

Run the automated test:

```powershell
.\scripts\test-kafka-messaging.ps1
```

This script will:
1. ✅ Verify Kafka infrastructure is running
2. ✅ List all topics
3. ✅ Show recent messages from each topic
4. ✅ Offer to start live consumer

**Interactive mode:**
- Choose a topic to monitor live
- Keep terminal open while triggering actions via REST API
- See events appear in real-time

---

## Test 6: Event Schema Validation

### Check Event Structure

All events should follow this schema:

```json
{
  "eventId": "UUID",
  "eventType": "EventName",
  "aggregateId": "UUID",
  "aggregateType": "Tenant|User|Role|Ledger|JournalEntry",
  "tenantId": "UUID",
  "timestamp": "ISO-8601",
  "version": 1,
  "data": {
    // Event-specific payload
  },
  "metadata": {
    "correlationId": "UUID",
    "causationId": "UUID",
    "userId": "UUID"
  }
}
```

### Verify Headers

```powershell
docker exec erp-redpanda rpk topic consume identity.tenants `
  --num 1 `
  --format '%h %v\n'
```

✅ **Expected headers:**
- `eventType`: Event name
- `aggregateId`: Entity ID
- `tenantId`: Tenant ID
- `timestamp`: Event time
- `correlationId`: Request tracking

---

## Test 7: Cross-Service Event Flow

### Scenario: Tenant Provisioning triggers downstream actions

1. **Identity Service publishes:** `TenantProvisioned` event
2. **Finance Service consumes:** Creates default chart of accounts
3. **Operations Service consumes:** Sets up initial configuration

**Test this:**

```powershell
# Terminal 1: Monitor all topics
docker exec erp-redpanda rpk topic consume identity.tenants --format json &
docker exec erp-redpanda rpk topic consume finance.ledgers --format json &

# Terminal 2: Create tenant
curl -X POST http://localhost:8081/api/v1/identity/tenants -H "Content-Type: application/json" -d '{"name":"Cross-Service Test","slug":"cross-test",...}'

# Check if Finance service reacts (future feature)
```

⚠️ **Note:** Cross-service event handling may not be implemented yet. This tests the infrastructure readiness.

---

## Test 8: Event Ordering & Partitioning

### Check Partition Strategy

```powershell
# See which partition each message went to
docker exec erp-redpanda rpk topic consume identity.tenants `
  --num 10 `
  --format 'Partition: %p, Offset: %o, Key: %k\n'
```

✅ **Expected:**
- Messages with same `tenantId` go to same partition
- Preserves ordering per tenant
- Different tenants can be processed in parallel

---

## Test 9: Consumer Groups (Future)

```powershell
# List consumer groups
docker exec erp-redpanda rpk group list

# Check consumer lag
docker exec erp-redpanda rpk group describe <group-name>
```

✅ **Expected groups:**
- `identity-service`
- `finance-service`
- `operations-service` (when implemented)

---

## Test 10: Kafka Performance Metrics

```powershell
# Check topic stats
docker exec erp-redpanda rpk topic describe identity.tenants

# Check broker stats
docker exec erp-redpanda rpk cluster health
```

✅ **Expected metrics:**
- **Replication factor:** 1 (dev), 3 (prod)
- **Partitions:** 3-5 per topic
- **Retention:** 7 days (168 hours)
- **Max message size:** 1MB

---

## Troubleshooting

### No messages appearing

```powershell
# Check if topic exists
docker exec erp-redpanda rpk topic list | Select-String "identity"

# Check service logs for publishing errors
# Look in colored PowerShell windows for each service
```

### Consumer not receiving messages

```powershell
# Check consumer offset
docker exec erp-redpanda rpk topic consume identity.tenants --offset start

# Reset consumer offset
docker exec erp-redpanda rpk group delete <group-name>
```

### Events have wrong schema

- Check event serialization in code
- Verify JSON schema registry
- Validate event version compatibility

---

## Success Criteria

- ✅ All topics created automatically
- ✅ Events published on tenant creation
- ✅ Events published on user creation
- ✅ Events published on journal entry
- ✅ Events contain correct schema
- ✅ Events include metadata headers
- ✅ Partitioning works correctly
- ✅ Consumer groups track offsets
- ✅ No message loss
- ✅ Events arrive in order per partition

---

## Next Steps

1. **Implement Event Consumers:** Finance service should consume `TenantProvisioned` events
2. **Add Saga Patterns:** Distributed transactions across services
3. **Event Sourcing:** Rebuild state from events
4. **CDC (Change Data Capture):** Sync with read models
5. **Dead Letter Queue:** Handle failed event processing

---

## Quick Reference

```powershell
# Run automated test
.\scripts\test-kafka-messaging.ps1

# List topics
docker exec erp-redpanda rpk topic list

# Consume live
docker exec erp-redpanda rpk topic consume <topic> --format json

# View last N messages
docker exec erp-redpanda rpk topic consume <topic> --num N --offset -N

# Check cluster health
docker exec erp-redpanda rpk cluster health
```
