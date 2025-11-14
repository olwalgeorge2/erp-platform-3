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

## Current Status (2025-11-14)
- ✅ **Domain** – Aggregates + invariants codified with tests (`ChartOfAccountsTest`, `JournalEntryTest`, `AccountingPeriodTest`, `MoneyTest`).
- ✅ **Application** – `AccountingCommandHandler` exposes CreateLedger / DefineAccount / PostJournalEntry / CloseAccountingPeriod / RunCurrencyRevaluation command paths.
- ✅ **Persistence** – Flyway migrations V001 (baseline schema), V004 (audit columns), V005 (exchange rates), V006 (original currency on journal lines).
- ✅ **API layer** – Finance service exposes `/api/v1/finance/**` REST endpoints; gateway proxy/scopes configured; REST samples at `docs/rest/finance-accounting.rest`.
- ✅ **Events** – Kafka publisher emits `finance.journal.events.v1` / `finance.period.events.v1` payloads aligned with the new JSON schemas (reconciliation topic reserved).
- ✅ **Observability** – Micrometer instrumentation (@Timed, @Counted, gauges) on all command handlers; readiness health checks at `/q/health/ready`.

## Integration Contracts
- **Inbound events** (planned): `identity.domain.events.v1`, `commerce.order.events.v1`, `procurement.payables.events.v1` to seed automatic postings.
- **Outbound events**: `finance.journal.events.v1`, `finance.period.events.v1`, `finance.reconciliation.events.v1` (schemas to be registered in `docs/schemas/finance/` per EVENT_VERSIONING_POLICY).
- **REST surface**: 
  - `POST /api/v1/finance/ledgers` - Create ledger
  - `POST /api/v1/finance/chart-of-accounts/{id}/accounts` - Define account
  - `POST /api/v1/finance/journal-entries` - Post journal entry
  - `POST /api/v1/finance/ledgers/{ledgerId}/periods/{periodId}/close` - Close accounting period
  - `POST /api/v1/finance/ledgers/{ledgerId}/periods/{periodId}/currency-revaluation` - Run FX revaluation (Phase 5A)
  - See `docs/rest/finance-accounting.rest` for ready-to-run HTTP examples.

## Security & Access
- JWT scopes reserved via gateway: `financial-admin` (full control), `financial-user` (posting + read), `financial-auditor` (read-only, multi-tenant as approved).
- Multi-tenancy enforced via tenant ID on every table + repository query filters; optimistic locking enabled on mutable aggregates.
- All APIs require `X-Tenant-Id` + `X-Request-Id` headers consistent with platform guidelines.

## Phase 5A Multi-Currency Foundation ✅
Sprint 0 stabilization and multi-currency foundation complete:
- ✅ Exchange rate database (`exchange_rates` table with ExchangeRateProvider/DatabaseExchangeRateProvider)
- ✅ Foreign currency journal line support (original_currency + original_amount columns)
- ✅ Automatic conversion to ledger base currency at posting time
- ✅ Unrealized gain/loss revaluation (`RunCurrencyRevaluationCommand` + REST endpoint)
- ✅ Micrometer instrumentation for all operations
- ✅ Readiness health checks ensuring DB connectivity
- ✅ Audit metadata (created_by, updated_by, source_system) with JPA lifecycle hooks
- ✅ REST API documentation with gateway-ready examples

## Next Steps (Phase 5A Continued)
1. **Cost Centers & Dimensions** (next priority): Add cost_center_id, project_id, department_id to journal lines for multi-dimensional reporting.
2. **Query/Reporting APIs**: Implement trial balance, GL summary, and period status endpoints for read-side operations.
3. **Approval Workflow MVP**: Add draft/submitted/approved/rejected states with workflow service and enforcement in command handlers.
4. **Scheduler for Currency Revaluation**: Monthly/quarterly automatic revaluation jobs triggered by scheduled task.
5. **Exchange Rate Management APIs**: REST endpoints for manual exchange rate entry and historical rate queries.

