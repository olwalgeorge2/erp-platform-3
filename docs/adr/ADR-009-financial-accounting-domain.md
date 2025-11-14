
# ADR-009: Financial Accounting Domain Strategy

**Status:** Accepted  
**Date:** 2025-11-13  
**Context:** Phase 4 – First financial slice (`financial-management/financial-accounting`)

## Decision
Establish `financial-accounting` as an autonomous bounded context responsible for the general ledger, chart of accounts, accounting periods, and journal entry posting. The context will:

- Maintain domain aggregates (`Ledger`, `ChartOfAccounts`, `AccountingPeriod`, `JournalEntry`, `PostingBatch`) with strict double-entry invariants.
- Persist to a dedicated schema `financial_accounting` (per ADR-002) with Flyway-managed migrations.
- Expose synchronous APIs via the API Gateway under `/api/v1/finance/**` guarded by new `financial-*` scopes.
- Publish/consume versioned events (`finance.journal.events.v1`, `finance.period.events.v1`) registered in Schema Registry per the event-versioning policy.
- Reuse platform conventions (Quarkus stack, transactional outbox, Micrometer/OTel, shared JWT/tenant headers) to meet SAP-grade availability/SLA targets.

### Aggregates & API Surfaces
- **Ledger** – owns base currency + chart-of-accounts linkage; API: `POST /api/v1/finance/ledgers`.
- **ChartOfAccounts & Account** – hierarchical account catalog; API: `POST /api/v1/finance/chart-of-accounts/{id}/accounts`.
- **AccountingPeriod** – open/freeze/close workflow; API: `POST /api/v1/finance/ledgers/{ledgerId}/periods`.
- **JournalEntry / PostingBatch** – double-entry capture; API: `POST /api/v1/finance/journal-entries`, `POST /api/v1/finance/posting-batches/{id}/submit`.

### Event Flows
- **Outbound**
  - `finance.journal.events.v1` – emitted after journal posting; consumed by BI, AP/AR, external ERPs.
  - `finance.period.events.v1` – broadcast on period freeze/close to trigger reporting.
  - `finance.reconciliation.events.v1` – optional for downstream compliance engines.
- **Inbound**
  - `identity.domain.events.v1` – to sync tenant/user metadata for audit logging.
  - `commerce.order.events.v1`, `procurement.payables.events.v1` – to auto-generate journal entries.

## Context
- Phase 4 requires a demonstrable financial slice before onboarding other contexts.
- Financial data has the highest integrity/compliance requirements (SOX, audit, GDPR retention).
- Existing ADRs (002, 003, 005, 006) demand strong isolation, clear contracts, and governance; we must extend those to finance.

## Alternatives Considered
1. **Embed financial logic into commerce / shared modules**  
   Rejected: Violates bounded-context autonomy and complicates compliance boundaries.
2. **Defer ledger modeling until later phases**  
   Rejected: Phase 4 success criteria explicitly require a vertical slice with measurable SLIs.
3. **Adopt event sourcing + bespoke storage from day one**  
   Deferred: Adds risk/complexity; we can layer event sourcing later on top of the CQRS baseline.

## Consequences
### Positive
- Clear aggregate boundaries with ADR documentation for future squads.
- Schema-level isolation keeps tenant financial data contained.
- Gateway/Auth scope additions align with existing security posture.
- Reusable event contracts enable downstream BI, AP/AR, and audit services.

### Negative
- More modules (domain/application/infrastructure) increase build graph size.
- Additional schema + migrations require DBA oversight.
- Need to meet tighter latency/perf targets (journal posting <200 ms p95, ledger queries <100 ms p95).

### Neutral
- Outbox/event strategy mirrors identity; operational effort stays consistent but requires additional monitoring boards.

## Compliance / Enforcement
1. **Schema & migrations** – Flyway scripts prefixed `V_FA_###` scoped to `financial_accounting`.
2. **Gateway scopes** – `financial-admin`, `financial-user`, `financial-auditor` enforced via JWT claims and gateway config.
3. **Observability** – Metrics following `OBSERVABILITY_BASELINE.md` (`finance.journal.post.duration`, etc.) with alert thresholds defined in `SECURITY_SLA.md`.
4. **QA gates** – Aggregate invariant tests (100% coverage), Quarkus integration tests (Testcontainers) executed with `-PwithContainers=true`.
5. **Event governance** – Schemas stored in `docs/schemas/finance/...` and validated per `EVENT_VERSIONING_POLICY.md`.

## Related ADRs
- [ADR-001](ADR-001-modular-cqrs.md) – CQRS conventions applied to ledger commands/queries.
- [ADR-002](ADR-002-database-per-context.md) – Schema-per-context strategy.
- [ADR-003](ADR-003-event-driven-integration.md) – Outbox/Event architecture reused.
- [ADR-005](ADR-005-multi-tenancy-isolation.md) – Tenant isolation rules (RLS/filters).
- [ADR-006](ADR-006-platform-shared-governance.md) – Limits on shared-kernel leakage.
