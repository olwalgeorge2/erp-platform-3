# Finance Schema Registry Summary

**Last Updated:** 2025-11-14  
**Registry Endpoint:** http://localhost:18081 (local Redpanda)  
**Registration Date:** 2025-11-14

## Registered Schemas

All finance event schemas have been successfully registered in the Schema Registry.

| Subject | Schema ID | Version | Schema Type | Event Type |
|---------|-----------|---------|-------------|------------|
| `finance.journal.events.v1-value` | **1** | 1 | JSON | Journal posting events |
| `finance.reconciliation.events.v1-value` | **2** | 1 | JSON | Reconciliation events |
| `finance.period.events.v1-value` | **3** | 1 | JSON | Accounting period events |

## Schema Details

### 1. Journal Events (Schema ID: 1)
- **Subject:** `finance.journal.events.v1-value`
- **Schema File:** `docs/schemas/finance/finance.journal.events.v1.json`
- **Event Types:**
  - `finance.journal.posted` - Emitted when journal entry is successfully posted to the ledger
- **Key Properties:**
  - `journalEntryId` (UUID) - Unique identifier for the journal entry
  - `ledgerId` (UUID) - Target ledger
  - `totalDebitsMinor` / `totalCreditsMinor` (integer) - Balanced amounts in minor units
  - `lines` (array) - Individual debit/credit line items with account references
  - `currency` (ISO 4217) - 3-letter currency code

### 2. Reconciliation Events (Schema ID: 2)
- **Subject:** `finance.reconciliation.events.v1-value`
- **Schema File:** `docs/schemas/finance/finance.reconciliation.events.v1.json`
- **Event Types:**
  - `finance.reconciliation.started` - Reconciliation process initiated
  - `finance.reconciliation.completed` - Reconciliation completed successfully
  - `finance.reconciliation.failed` - Reconciliation encountered errors

### 3. Period Events (Schema ID: 3)
- **Subject:** `finance.period.events.v1-value`
- **Schema File:** `docs/schemas/finance/finance.period.events.v1.json`
- **Event Types:**
  - `finance.period.closed` - Accounting period closed for posting
  - `finance.period.reopened` - Previously closed period reopened
- **Key Properties:**
  - `periodId` (UUID) - Unique identifier for the accounting period
  - `status` (enum) - Period status (OPEN, CLOSED, LOCKED)
  - `closedBy` (UUID) - User who performed the closure

## Usage in Code

### Producer Configuration

The `KafkaFinanceEventPublisher` in `accounting-infrastructure` references these schemas:

```kotlin
// Schema IDs are automatically resolved by the Schema Registry
// when using JSON Schema serialization with schema.registry.url configured

@ApplicationScoped
class KafkaFinanceEventPublisher(
    @Channel("finance-journal-events") 
    private val journalEmitter: Emitter<String>,
    
    @Channel("finance-period-events") 
    private val periodEmitter: Emitter<String>
) : FinanceOutboxMessagePublisher {
    
    override fun publishJournalPosted(event: FinanceJournalPostedEvent) {
        // Message is validated against Schema ID 1
        journalEmitter.send(Message.of(event.toJson()))
    }
    
    override fun publishPeriodClosed(event: FinancePeriodClosedEvent) {
        // Message is validated against Schema ID 3
        periodEmitter.send(Message.of(event.toJson()))
    }
}
```

### Consumer Configuration

Downstream consumers (e.g., reporting, analytics) should reference the schema by ID or subject:

```yaml
# application.yml for consumers
mp.messaging.incoming.finance-journal-events:
  connector: smallrye-kafka
  topic: finance.journal.events.v1
  value.deserializer: io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer
  schema.registry.url: http://localhost:18081
```

## Registration Process

Schemas were registered using the PowerShell registration script:

```powershell
# Local registration
.\scripts\register_finance_schemas.ps1

# Production registration (when available)
.\scripts\register_finance_schemas.ps1 `
  -RegistryUrl "https://schema-registry.prod.erp.local" `
  -BasicAuth "<base64-encoded-credentials>"
```

### Registration Script Details
- **Script:** `scripts/register_finance_schemas.ps1`
- **Bash Version:** `scripts/register_finance_schemas.sh` (for Unix/Linux)
- **Schema Source:** `docs/schemas/finance/*.json`
- **Subject Naming:** `<domain>.<aggregate>.<event-type>.<version>-value`

## Verification

Verify registration via Schema Registry API:

```powershell
# List all subjects
Invoke-RestMethod -Uri "http://localhost:18081/subjects"

# Get specific schema
Invoke-RestMethod -Uri "http://localhost:18081/subjects/finance.journal.events.v1-value/versions/1"

# Check compatibility
Invoke-RestMethod -Uri "http://localhost:18081/compatibility/subjects/finance.journal.events.v1-value/versions/latest" `
  -Method Post `
  -ContentType "application/vnd.schemaregistry.v1+json" `
  -Body '{"schema": "{...}"}'
```

Or use the Redpanda Console UI: http://localhost:8090

## Schema Evolution

Per ADR-007 and the Event Versioning Policy (`docs/EVENT_VERSIONING_POLICY.md`):

1. **Backward Compatible Changes** (minor version bump):
   - Add optional fields
   - Remove enum values (if consumers handle unknown values)
   - Widen field constraints (e.g., increase maxLength)

2. **Breaking Changes** (major version bump):
   - Remove required fields
   - Change field types
   - Rename fields
   - Add new required fields

For breaking changes, create a new subject (e.g., `finance.journal.events.v2-value`) and maintain parallel publishing during migration.

## Production Deployment

When deploying to production environments:

1. **Update Registry URL:**
   ```bash
   export SCHEMA_REGISTRY_URL=https://schema-registry.prod.erp.local
   export SCHEMA_REGISTRY_BASIC_AUTH=$(echo -n "username:password" | base64)
   ```

2. **Run Registration:**
   ```bash
   ./scripts/register_finance_schemas.sh
   ```

3. **Update Application Config:**
   ```yaml
   kafka:
     schema:
       registry:
         url: ${SCHEMA_REGISTRY_URL}
   ```

4. **Verify Deployment:**
   - Check schema IDs match across environments
   - Validate compatibility mode settings
   - Confirm producer/consumer can serialize/deserialize

## Related Documentation

- **Schema Playbook:** `docs/SCHEMA_REGISTRY_PLAYBOOK.md`
- **Event Versioning:** `docs/EVENT_VERSIONING_POLICY.md`
- **Finance Domain ADR:** `docs/adrs/ADR-009-financial-accounting-domain.md`
- **Outbox Pattern:** `bounded-contexts/financial-management/financial-accounting/accounting-infrastructure/src/main/kotlin/com/erp/finance/accounting/infrastructure/outbox/README.md`

## Next Steps

- [ ] Update `PHASE4_READINESS.md` with schema registration completion
- [ ] Configure production Schema Registry endpoint
- [ ] Add schema compatibility tests to CI pipeline
- [ ] Implement schema evolution procedures in deployment runbooks
- [ ] Add monitoring alerts for schema validation failures
