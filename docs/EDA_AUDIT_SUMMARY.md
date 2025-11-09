# EDA Audit Summary

**Date**: November 9, 2025  
**Scope**: Event-Driven Architecture implementation across 12 bounded contexts  
**Output**: ADR-007 Event-Driven Architecture Hybrid Policy

## Audit Findings

### Tenancy-Identity Context (✅ Implemented)

**Pattern**: Hybrid (Async Events + Sync REST)

#### Event Infrastructure
- ✅ **Domain Events**: 4 events defined
  - `UserCreatedEvent`
  - `UserUpdatedEvent`
  - `TenantProvisionedEvent`
  - `RoleAssignedEvent`
- ✅ **Port Abstraction**: `EventPublisherPort` interface
- ✅ **Outbox Pattern**: Full implementation with transactional guarantees
  - `OutboxEventEntity` (JPA entity)
  - `OutboxEventPublisher` (port implementation)
  - `OutboxEventScheduler` (5-second polling)
  - `KafkaOutboxMessagePublisher` (Kafka adapter)
- ✅ **Kafka Integration**: 
  - Channel: `identity-events-out`
  - Topic: `identity.domain.events.v1`
  - Partitioning by aggregate ID
  - Headers: event-type, tenant-id, trace-id, aggregate-id, event-version (from DomainEvent)
  - DLQ: `identity.domain.events.dlq` (for events exceeding max attempts)
- ✅ **Observability**:
  - Metrics: `identity.outbox.events.published` (success/failure), `identity.outbox.publish.duration`, `identity.outbox.batch.duration`, `identity.outbox.events.pending` (gauge)
  - Structured logging with MDC (traceId, tenantId)
  - ACK/NACK handling
  - Retry logic with outbox reprocessing (max 5 attempts)
  - Health monitoring via pending events gauge

#### Synchronous Integration
- REST endpoints for authentication, tenant management, role queries
- Request-response pattern for real-time operations

**Assessment**: Production-ready hybrid pattern with excellent separation of concerns

### Other Contexts (❌ Not Implemented)

- **Financial Management**: Placeholder `EventPublisherPort` objects only
- **Commerce**: Placeholder event files, no infrastructure
- **Manufacturing**: Placeholder event files, no infrastructure
- **Inventory**: Placeholder event files, no infrastructure
- **Customer Relation**: No event infrastructure
- **All Others**: No event infrastructure

**Current State**: REST-only integration across non-identity contexts

## Research-Based Analysis

### Codebase Deep Dive
- **Modules Analyzed**: 74 modules across 12 bounded contexts
- **Implementation Status**: Only tenancy-identity (1/12) has EDA implementation
- **Code State**: 11 contexts have placeholder-only code (opportunity for greenfield implementation)
- **Integration Requirements**: Analyzed CONTEXT_MAP.md for upstream/downstream dependencies

### ERP Integration Research
Studied industry patterns from:
- SAP S/4HANA event-driven architecture
- Oracle SOA Suite ERP integration patterns
- Microsoft Dynamics 365 dataverse events
- NetSuite SuiteFlow workflows

### Key Findings
1. **Order-to-Cash**: High event volume, requires async choreography
2. **Procure-to-Pay**: Long-running workflows benefit from events
3. **Make-to-Order**: Mixed sync queries (BOM) + async updates (production status)
4. **Master Data**: Hybrid pattern optimal (sync queries + async updates)
5. **Analytics**: Pure consumer pattern for BI
6. **Transactional Consistency**: Outbox pattern mandatory for financial accuracy

## Policy Decision

Created **ADR-007: Event-Driven Architecture Hybrid Policy** defining three integration patterns:

### 1. Pure Event-Driven (Pure EDA)
- All integration via domain events
- No synchronous APIs
- High decoupling, eventual consistency
- **Use Case**: Business Intelligence (event consumer aggregating from all contexts)

### 2. Hybrid (Events + REST) ← **Recommended Default**
- Commands/writes publish events (async)
- Queries exposed via REST APIs (sync)
- CQRS separation
- **Use Cases**: Identity, Commerce, Inventory, Financial, Customer, Manufacturing, Procurement

### 3. Synchronous (REST-Only)
- Request-response only
- Real-time consistency required
- Low integration volume
- **Use Cases**: Communication Hub, Corporate Services, API Gateway

## Implementation Guidelines Provided

### Event Publishing
1. Domain event definitions (data classes implementing `DomainEvent`)
2. Port abstraction (`EventPublisherPort` interface)
3. Transactional outbox pattern (MANDATORY for reliability)
4. Outbox scheduler (background polling with retry logic)
5. Kafka message publisher (with partitioning and headers)

### Event Consumption
1. Event consumer with idempotent handling
2. ACK/NACK message acknowledgment
3. Event type routing via headers
4. Error handling with DLQ fallback

### Configuration
- Kafka topic naming: `{context}.domain.events.v{version}`
- Consumer group IDs: `{context}-{source}-consumer`
- Application.yml configuration examples

## Migration Playbook

### REST-Only → Hybrid
1. Add Kafka dependencies
2. Create domain events
3. Implement outbox pattern
4. Wire event publisher to use cases
5. Configure Kafka channels
6. Keep REST APIs (backward compatible)

### Hybrid → Pure EDA (Rare)
1. Ensure consumers have event-based read models
2. Deprecate REST endpoints
3. Monitor zero traffic
4. Remove endpoints after sunset

### Pure EDA → Hybrid
1. Add query services
2. Create REST endpoints (queries only)
3. Keep event publishing unchanged

## Governance Rules

### ArchUnit Tests (Provided)
- Event publishers must use outbox pattern
- Domain events must be immutable
- EventPublisherPort must reside in application layer
- Application layer must not depend on Kafka directly

### Naming Conventions
- Events: `{Aggregate}{Action}Event` (past tense)
- Topics: `{context}.domain.events.v{version}`
- Packages: `{context}.domain.events`

### Monitoring Requirements
- Metrics: published count, duration, pending, failed
- Logs: publish/consume success/failure, retries, DLQ
- Health checks: Kafka connectivity, outbox lag

## Recommended Pattern by Context

| Context | Pattern | Status |
|---------|---------|--------|
| Tenancy-Identity | Hybrid | ✅ Implemented |
| Financial Management | Hybrid | 📋 Planned |
| Commerce | Hybrid | 📋 Planned |
| Inventory | Hybrid | 📋 Planned |
| Customer Relation | Hybrid | 📋 Planned |
| Manufacturing | Hybrid | 📋 Planned |
| Procurement | Hybrid | 📋 Planned |
| Operations Service | Hybrid | 📋 Planned |
| Business Intelligence | Pure EDA (Consumer) | 📋 Planned |
| Communication Hub | REST-Only | ✅ Current (no change) |
| Corporate Services | REST-Only | ✅ Current (no change) |

## Next Steps

1. **Team Review**: Present ADR-007 for architecture team consensus
2. **Governance Integration**: Add ArchUnit rules to enforce EDA patterns
3. **Rollout Planning**: Prioritize contexts for hybrid migration (suggest: Commerce → Inventory → Financial)
4. **Template Creation**: Extract tenancy-identity implementation as starter template
5. **Training**: Conduct EDA workshops for development teams
6. **Infrastructure**: Provision Kafka clusters and monitoring tools

## Files Created

- `docs/adr/ADR-007-event-driven-architecture-hybrid-policy.md` - Comprehensive policy document (22KB) with:
  - **Research Foundation**: Industry ERP patterns and anti-patterns
  - **Pattern Definitions**: Pure EDA, Hybrid (⭐ recommended), REST-Only
  - **Context-Specific Recommendations**: All 12 contexts with detailed rationale
  - **Implementation Priority Matrix**: Phase-based rollout (18-20 weeks total)
  - **ERP Workflow Examples**: 4 real-world choreography patterns:
    - Order-to-Cash (Commerce → Inventory → Financial)
    - Procure-to-Pay (Procurement → Inventory → Financial)
    - Make-to-Order (Commerce → Manufacturing → Inventory)
    - Customer Support (Customer → Operations → Communication)
  - **Implementation Guidelines**: Complete code examples with:
    - Domain events (immutable data classes)
    - Port abstractions (EventPublisherPort)
    - Transactional outbox pattern (mandatory)
    - Kafka integration (partitioning, headers, ACK/NACK)
    - Event consumers (idempotent handling)
  - **Migration Playbook**: REST-Only → Hybrid → Pure EDA
  - **Governance Rules**: ArchUnit tests for enforcement
  - **Monitoring Requirements**: Metrics, logs, health checks
  - **Consequences**: Trade-offs and mitigations

- `docs/EDA_AUDIT_SUMMARY.md` - Executive summary with audit findings and recommendations

