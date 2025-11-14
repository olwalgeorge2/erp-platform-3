# Financial Accounting Context

## Purpose
- Maintain the tenant-scoped **Chart of Accounts**, ledgers, and accounting periods.
- Enforce double-entry **JournalEntry** invariants and posting workflows (draft → approved → posted).
- Provide APIs for journal capture, ledger queries, and period close operations while meeting SAP-grade SLAs (journal posting < 200 ms p95, ledger queries < 100 ms p95).
- Publish `finance.journal.events.v1` (and follow-on topics) for downstream AP/AR, BI, and audit services; consume identity/account events for enrichment.

## Module Layout
```
financial-accounting/
  accounting-domain/          // Aggregates, value objects, domain services
  accounting-application/     // CQRS command/query handlers, use cases
  accounting-infrastructure/  // REST adapters, persistence, messaging, Flyway migrations
```

## Current Status (2025-11-13)
- ✅ **Domain** – Aggregates + invariants codified with tests (`ChartOfAccountsTest`, `JournalEntryTest`, `AccountingPeriodTest`).
- ✅ **Application** – `AccountingCommandHandler` exposes CreateLedger / DefineAccount / PostJournalEntry / CloseAccountingPeriod command paths.
- ✅ **Persistence** – Flyway baseline `V001__create_financial_accounting_schema.sql` plus JPA repositories for ledgers, chart of accounts, periods, and journal entries.
- 🟡 **API layer** – Finance service now exposes `/api/v1/finance/**` REST endpoints; gateway proxy/scopes still pending.
- ✅ **Events** – Kafka publisher emits `finance.journal.events.v1` / `finance.period.events.v1` payloads aligned with the new JSON schemas (reconciliation topic reserved).
- 🟡 **Observability** – Micrometer + logging hooks to be wired before load/perf validation.

## Integration Contracts
- **Inbound events** (planned): `identity.domain.events.v1`, `commerce.order.events.v1`, `procurement.payables.events.v1` to seed automatic postings.
- **Outbound events**: `finance.journal.events.v1`, `finance.period.events.v1`, `finance.reconciliation.events.v1` (schemas to be registered in `docs/schemas/finance/` per EVENT_VERSIONING_POLICY).
- **REST surface**: `/api/v1/finance/ledgers`, `/api/v1/finance/chart-of-accounts/{id}/accounts`, `/api/v1/finance/journal-entries`, `/api/v1/finance/ledgers/{ledgerId}/periods/{periodId}/close`.

## Security & Access
- JWT scopes reserved via gateway: `financial-admin` (full control), `financial-user` (posting + read), `financial-auditor` (read-only, multi-tenant as approved).
- Multi-tenancy enforced via tenant ID on every table + repository query filters; optimistic locking enabled on mutable aggregates.
- All APIs require `X-Tenant-Id` + `X-Request-Id` headers consistent with platform guidelines.

## Next Steps
1. Register finance event schemas in the platform registry + back the publisher with the shared outbox pattern.
2. Update API Gateway routes/scopes and add integration tests for `/api/v1/finance/**`.
3. Add Micrometer metrics, health checks, and structured logging correlated with gateway/identity MDC fields.
4. Extend documentation (`PHASE4_READINESS.md`, `SECURITY_SLA.md`, `CONTEXT_MAP.md`) as milestones complete.
