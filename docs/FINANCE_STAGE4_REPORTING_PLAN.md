# Finance Stage 4 Execution Plan - Reporting Suite

_Last updated: 2025-11-14_  
_Scope window: Phase 5D (Sprints 5-6)_  
_Source blueprint:_ `docs/FINANCE_SAP_PARITY_EXECUTION.md#5-stage-4-reporting-suite`

Stage 4 delivers enterprise reporting comparable to SAP, leveraging the dimensional, sub-ledger, and tax data created in earlier stages.

---

## 1. Goals & Non-Goals

**Goals**
- Provide out-of-the-box financial statements (P&L, Balance Sheet, Cash Flow direct/indirect).
- Offer AR/AP aging, drill-down trial balance, management P&L by dimension, and cash analytics.
- Ensure performance (sub-10s for 1M-line ledgers) via caching/materialized views.
- Enable exports (CSV/XLSX/JSON) and APIs suitable for BI tooling.

**Non-goals**
- Does not replace enterprise BI suites; focus on finance reporting.
- No additional workflow (approvals) beyond data retrieval.
- No predictive analytics (tracked for later phases).

---

## 2. Architecture & Data Model Deliverables

| Item | Description | Owners |
|------|-------------|--------|
| Reporting schema (`reporting_snapshots`, `statement_definitions`, `statement_lines`) | Stores generated statement data with metadata | Reporting squad |
| Materialized views / OLAP cubes | Aggregations per dimension, ledger, period | Data engineering |
| Cache invalidation workers | Listen to GL/sub-ledger/tax events to refresh snapshots | Reporting squad |

Statement definition model:
- Supports templates per GAAP/IFRS; configurable line definitions with dimension filters and formula expressions.
- Each statement snapshot tracks source dataset hash for audit.

---

## 3. Services & APIs

1. **Reporting Service**
   - `/api/v1/finance/reporting/statements/{statementType}` with query params for company code, fiscal period, dimension filters.
   - `/api/v1/finance/reporting/trial-balance` with drill-down capability.
   - `/api/v1/finance/reporting/aging/{ledger}` for AR/AP aging buckets.
   - `/api/v1/finance/reporting/export` supporting CSV/XLSX/JSON and signed URLs.

2. **Statement Definition APIs**
   - Manage templates, formulas, and line ordering.
   - Allow cloning SAP-style chart of account groupings for quick onboarding.

3. **Scheduler / Snapshot Builder**
   - Nightly job generates statement snapshots per company code and fiscal period, with incremental updates on demand.
   - On-demand rebuild endpoint for auditors.

4. **Integration Connectors**
   - CDC or REST connectors to push data into Snowflake/Superset; document usage.

---

## 4. Performance & Observability

- Benchmarks: P&L < 10s (1M lines), drill-down < 400ms after cache warm-up, exports stream within 5s.
- Metrics: `reporting.snapshot.duration`, `reporting.query.latency`, `reporting.cache.hit_ratio`, `reporting.export.count`.
- Dashboards: Statement generation backlog, cache freshness, export success/failures.
- Logging: Include statement id, parameters, dataset hashes for traceability.

---

## 5. Testing Strategy

| Layer | Tests |
|-------|-------|
| Unit | Statement formula evaluation, dimension filtering, cache invalidation logic. |
| Integration | Snapshot builder hitting Postgres/OLAP, verifying data accuracy. |
| Contract | REST API schema validation + export format verification. |
| Performance | Load tests with 1M+ journal lines; regression monitoring on latency. |

Add `docs/FINANCE_LIVE_TEST_GUIDE.md` scenarios:
- Generate P&L and Balance Sheet for multi-company tenant.
- Drill-down from trial balance to journal lines.
- Export AR aging for 10k open items.

---

## 6. Definition of Ready / Done

**Ready**
- Upstream data (dimensions, sub-ledgers, tax) stable with final event schemas.
- Statement templates defined for GAAP and IFRS pilot tenants.
- Hardware/infra sizing approved for OLAP workloads.

**Done**
- Reporting service deployed with full API coverage and documentation.
- Snapshot builder + cache invalidation running with monitoring.
- Performance targets met in staging with synthetic and anonymized real data.
- Exports validated by finance SMEs; numbers reconcile with GL/sub-ledgers.
- Docs updated: this plan, REST specs, runbooks, live test guide.

---

## 7. Timeline & Ownership

| Week | Focus | Exit Criteria |
|------|-------|---------------|
| Week 1 | Schema + statement template tooling | Statement definition APIs merged, migrations applied |
| Week 2 | Snapshot builder + cache layer | Nightly job running, cache invalidation wired to events |
| Week 3 | Reporting APIs + exports | P&L/BS/Cash Flow endpoints returning data, exports stable |
| Week 4 | Performance tuning + BI connectors | Benchmarks met, connectors documented |
| Week 5 | Hardening + UAT | Finance SMEs sign off on reports, dashboards published |

Owners: Reporting/Analytics squad, Data engineering (OLAP), DevEx (performance testing), QA/Finance SMEs.

---

## 8. Cutover Checklist

1. Precompute statement snapshots for historical periods (at least last 2 fiscal years).
2. Validate reconciliation between reporting snapshots and GL/sub-ledger source-of-truth.
3. Train finance teams on new APIs/exports; publish sample queries.
4. Enable reporting feature flags per tenant once Stage 3 acceptance is done.
5. Update change log and release notes highlighting statement availability and SLAs.

Completing Stage 4 brings the reporting score close to SAP parity and paves the way for automation in Stage 5.

