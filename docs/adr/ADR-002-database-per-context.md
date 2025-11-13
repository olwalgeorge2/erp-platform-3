# ADR-002: Database Per Bounded Context

**Status**: Accepted  
**Date**: 2025-11-05  
**Deciders**: Architecture Team, Platform Team  
**Tags**: database, persistence, bounded-contexts, microservices

## Context

In a microservices architecture with 11 bounded contexts, we must decide on our database isolation strategy. The choice impacts data autonomy, scalability, deployment independence, and operational complexity.

## Decision

We will implement **logical database separation** using PostgreSQL **schemas per bounded context** with a path toward **physical database separation** for contexts requiring it.

### Strategy

1. **Initial Implementation (Phase 4-5)**
   - Single PostgreSQL instance
   - Separate schema per bounded context
   - Schema naming: `{context_name}` (e.g., `financial_accounting`, `inventory_stock`)
   - Each context owns its schema exclusively
   - No cross-schema foreign keys
   - No direct cross-schema queries

2. **Evolution Path (Phase 6+)**
   - High-traffic contexts (Commerce, Inventory) → dedicated database instances
   - Regulatory isolation contexts (Financial) → separate encrypted databases
   - Keep lower-traffic contexts on shared instance

3. **Access Control**
   - Each service has a dedicated database user
   - Users have access only to their context's schema
   - Principle of least privilege enforced

## Rationale

### Why Schemas First?
- ✅ **Simpler Operations**: Single PostgreSQL instance to manage initially
- ✅ **Lower Cost**: Reduced infrastructure overhead
- ✅ **Faster Development**: Easier local development setup
- ✅ **Backup/Restore**: Simplified disaster recovery
- ✅ **Clear Migration Path**: Can split to separate DBs later

### Why Not Shared Tables?
- ❌ Tight coupling between contexts
- ❌ Schema evolution conflicts
- ❌ Difficult to scale independently
- ❌ No clear ownership boundaries

### Why Not Separate Databases Immediately?
- ❌ Operational complexity too high for Phase 1-3
- ❌ Cost prohibitive during development
- ❌ Over-engineering before traffic patterns known

## 2025-11-13 Addendum (Schema Isolation Review)

### Identity/Tenancy Context
- **Current state:** Runs in shared Postgres instance using schema `tenancy_identity`; Flyway migrations scoped to that schema (see `bounded-contexts/tenancy-identity/identity-infrastructure/src/main/resources/db/migration`).
- **Rationale for staying on schema model (Phase 2-3):**
  - Strong coupling to other contexts is minimal; identity owns its tables and exposes everything via APIs/events.
  - Operational work (backups, PITR) already bundles all schemas; splitting now would add HA complexity before we have cross-context load.
  - Schema-level isolation satisfies current compliance requirements when combined with per-user DB credentials.
- **Migration trigger:** When concurrency exceeds 2k RPS sustained or regulatory guidance mandates encryption boundary per tenant, move Tenancy‑Identity to its own Postgres instance. No code changes required—update connection URL + credentials.
- **Schema naming:** `tenancy_identity` is reserved for this context across all environments. No other context may create objects inside it.

### Enforcement
- Database role `tenancy_identity_app` has privileges limited to `tenancy_identity.*`.
- CI check ensures Flyway migrations only touch `tenancy_identity` objects.
- Observability dashboards track per-schema size + slow queries (ties into `docs/OBSERVABILITY_BASELINE.md`).

---

## Consequences

### Positive
- ✅ Clear data ownership per bounded context
- ✅ Independent schema evolution
- ✅ Can migrate to separate DBs without code changes
- ✅ Simpler local development (one DB to run)
- ✅ Reduced operational burden initially
- ✅ Transaction support within a context

### Negative
- ❌ No distributed transactions across contexts
- ❌ Eventual consistency required for cross-context operations
- ❌ Single point of failure (mitigated by PostgreSQL HA)
- ❌ Schema-level isolation weaker than DB-level
- ❌ Noisy neighbor risk (one context impacts others)

### Neutral
- ⚖️ Requires discipline to not use cross-schema queries
- ⚖️ Need monitoring per schema
- ⚖️ Backup strategy must support schema-level restore

## Implementation Details

### Schema Creation
```sql
-- Executed during context bootstrap
CREATE SCHEMA IF NOT EXISTS financial_accounting;
CREATE SCHEMA IF NOT EXISTS inventory_stock;
-- ... one per context
```

### Connection Configuration
```yaml
# In bounded-contexts/financial-management/financial-accounting/config
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/erp_platform
quarkus.datasource.username=financial_accounting_user
quarkus.datasource.password=${DB_PASSWORD}
quarkus.hibernate-orm.database.default-schema=financial_accounting
```

### Migration Tool
- **Flyway** for schema migrations
- Migration scripts per context: `bounded-contexts/{context}/migrations/`
- Version prefix: `V{context_short}_{version}__description.sql`
- Example: `V_FIN_001__create_accounts_table.sql`

### Cross-Context Data Access
```kotlin
// ❌ NEVER DO THIS
@Query("SELECT * FROM inventory_stock.products WHERE ...")

// ✅ DO THIS INSTEAD
// Publish domain event
eventPublisher.publish(OrderPlacedEvent(orderId, productIds))

// Subscribe in Inventory context
@ConsumeEvent("OrderPlaced")
fun onOrderPlaced(event: OrderPlacedEvent) {
    // Update inventory in own schema
}
```

## Migration to Separate Databases

When a context needs its own database instance:

1. Create new PostgreSQL instance
2. Dump schema from shared database
3. Restore to new database
4. Update connection configuration
5. Update deployment manifests
6. Zero code changes required

## Monitoring & Observability

- Schema-level metrics (table sizes, query performance)
- Connection pool per context
- Slow query logs tagged with schema
- Backup verification per schema

## Alternatives Considered

### 1. Fully Shared Database
**Rejected**: Violates bounded context autonomy, creates coupling, prevents independent evolution.

### 2. Separate Databases from Day One
**Rejected**: Operational complexity too high for early phases, cost prohibitive, over-engineering.

### 3. NoSQL Per Context
**Rejected**: ERP requirements (ACID, complex queries, reporting) better suited to PostgreSQL. May consider for specific contexts later.

### 4. Event Sourcing with Event Store
**Deferred**: Excellent pattern but adds complexity. CQRS provides foundation; can add event sourcing to specific contexts later.

## Related ADRs

- ADR-001: Modular CQRS Implementation
- ADR-003: Event-Driven Integration Between Contexts (to be written)
- ADR-005: Multi-Tenancy Data Isolation (to be written)

## Review Date

- **Review After Phase 5**: Assess which contexts need dedicated databases
- **Review After Performance Testing**: Identify noisy neighbor issues
- **Review Quarterly**: Re-evaluate as traffic patterns emerge
