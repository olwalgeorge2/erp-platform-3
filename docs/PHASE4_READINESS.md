# Phase 4 Readiness Checklist - First Vertical Slice

_Last updated: 2025-11-13_

## 1. Candidate Context
- **Selected bounded context:** `financial-management/financial-accounting`
- **Why first:** Generates immediate business value (ledger, journal posting), has clear upstream dependencies (identity, gateway, platform-shared libraries), and exercises both synchronous APIs + event ingestion.

## 2. Entry Criteria
| # | Criterion | Status |
|---|-----------|--------|
| 1 | API Gateway + Tenancy-Identity in place with published contracts (Phase 2 complete) | ✅ Ready – gateway + identity services power current smoke suites |
| 2 | Data/messaging backbone documented w/ retention + recovery plans (Phase 3 Task 4.4) | 🟡 Updating – add finance RPO/RTO + schema registry notes |
| 3 | Schema isolation strategy ratified (ADR-002 addendum) | ✅ Complete – `financial_accounting` schema + Flyway baseline |
| 4 | Event versioning policy + registry workflow defined | 🟡 Pending registration – finance schemas to publish |
| 5 | Security SLAs + RPO/RTO targets agreed (docs/SECURITY_SLA.md) | 🟡 Tie-in required – add finance slice row |
| 6 | Development environment supports new context scaffolding (Gradle settings, shared plugins) | ✅ Complete – modules compile via `erp.quarkus-conventions` |
| 7 | Operational runbooks exist for gateway & identity (observability baseline) | 🟡 Extend – append finance escalation flow |
| 8 | Portal spike feedback captured (Phase 2 Task 3.6) | ⏳ Deferred – pending portal write-up |

## 3. Deliverables for the Slice
1. **Domain modeling** ✅ – Aggregates + invariants published in accounting-domain with unit coverage.
2. **Application layer** ✅ – Command handler + DTO plumbing live; query/event side next.
3. **Persistence adapters** ✅ – `financial_accounting` schema + JPA repositories aligned to ADR-002.
4. **API exposure** 🟡 – Service exposes `/api/v1/finance/**`; gateway proxy + scopes pending.
5. **Event integration** 🟡 – Kafka publisher emits `finance.journal/period.events.v1`; schema registry + outbox wiring remain.
6. **Testing** 🟡 – Domain tests green; infra/contract suites to add via Testcontainers.
7. **SLIs** 🟡 – Targets defined (<200 ms JE, <100 ms ledger); need Micrometer metrics + load tests.

## 4. Risks & Mitigations
- **Dependence on accounting SMEs:** schedule domain discovery workshops up front (docs/ROADMAP §5.1).
- **Shared schema growth:** enforce schema-per-context migrations from day one.
- **Event fan-out:** ensure versioning policy is followed before publishing `finance.*` topics.
- **Financial data integrity:** leverage optimistic locking, ledger reconciliation jobs.

## 5. Immediate Next Steps
1. ✅ Modeling session + ADR-009 completed; capture SME follow-ups.
2. ✅ Modules + Flyway baseline merged (keep migrations incremental).
3. 🟡 Wire `/api/v1/finance/**` through api-gateway with financial-* scopes.
4. 🟡 Update ROADMAP/SECURITY_SLA + observability docs once gateway + metrics land.
5. 🟡 Register finance schemas in the platform registry and connect the publisher to the shared outbox + Testcontainers harness.
   - Finance service still emits directly through `accounting-infrastructure/src/main/kotlin/com/erp/finance/accounting/infrastructure/adapter/output/event/KafkaFinanceEventPublisher.kt`; we need an outbox entity/repository/migration mirroring `tenancy-identity/identity-infrastructure/src/main/kotlin/com.erp.identity.infrastructure/outbox/**`.
   - Finance lacks an integration suite that boots Postgres + Kafka via Testcontainers (see the identity REST integration tests in `tenancy-identity/identity-infrastructure/src/test/kotlin/com/erp/identity/infrastructure/adapter/input/rest/*IntegrationTest.kt`).

## 6. Execution Plan
### Step 1 – Domain Foundations (Status: ✅ Complete / SME notes pending)
- ADR-009 accepted; append SME clarifications + ChartOfAccounts variants.
- Context map + README updated with aggregates and flows.
- Aggregate invariant tests merged (JournalEntryTest, ChartOfAccountsTest).

### Step 2 – Schema & Migrations (Status: ✅ Complete)
- Flyway baseline `V001__create_financial_accounting_schema.sql` with tenant indexes/FKs.
- Document partitioning/backfill approach for high-volume tenants in ADR-009 appendix.

### Step 3 – Application Layer (Status: 🟡 In Progress)
- Command handlers delivered; add query handlers + mapper coverage.
- Wire transactional outbox + Kafka publisher (replace logging shim).
- Extend unit tests to cover failure paths + optimistic locking cases.

### Step 4 – Infrastructure & APIs (Status: 🟡 In Progress)
- Service endpoints live; add API Gateway route/scopes + security tests.
- Configure health checks, Micrometer meters, and MDC-enriched logging.

### Step 5 – Event & Integration (Status: 🟡 In Progress)
- JSON schemas committed (`docs/schemas/finance/**`) and Kafka publisher live on finance service.
- Register schemas in the shared registry + hook publisher to transactional outbox and Kafka Testcontainers coverage.

### Step 6 – Non-Functional Gates (Status: 🔜 Not Started)
- Build load scripts (k6/JMeter) validating p95 targets before GA.
- Update SECURITY_SLA.md with finance SLO row + alert thresholds; publish dashboards.

### Step 7 – Documentation & Handover (Status: 🟡 Rolling)
- Keep ROADMAP, CONTEXT_MAP, ADR index synced as milestones land.
- Draft operational runbook (deploy/migrate/troubleshoot) pre-GA.
- Prepare onboarding/training outline for Finance squad.







