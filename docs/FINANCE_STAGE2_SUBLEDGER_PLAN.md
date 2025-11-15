# Finance Stage 2 Execution Plan - Sub-Ledger Foundations

_Last updated: 2025-11-14_  
_Scope window: Phase 5B (Sprints 3-4)_  
_Source blueprint:_ `docs/FINANCE_SAP_PARITY_EXECUTION.md#3-stage-2-sub-ledger-foundations`

Stage 2 introduces the SAP-style sub-ledger stack (AP, AR, Asset Accounting, Bank Accounting) and ensures every posting stays in sync with the general ledger and dimensional data created in Stage 1.

---

## 0. Status Snapshot (2025-11-14)

- [done] Vendor + customer master data APIs (new AP/AR services) with Flyway-backed schemas and dimension-aware defaults are merged.
- [done] AP invoice schema + command/REST layer (create/post/pay/list) landed with open-item stubs and sample calls in `docs/rest/finance-ap-ar.rest`.
- [next] Extend AP invoices into open-item aging + payment proposals, then mirror the pattern for AR invoices/receipts.

---

## 1. Goals & Non-Goals

**Goals**
- Deliver AP and AR modules with shared vendor/customer masters, open-item tracking, and payment/receipt flows.
- Wire AP/AR postings into GL using the existing command bus + transactional outbox, including dimension metadata.
- Extend the ledger schema with sub-ledger references for traceability.
- Add Asset Accounting MVP (depreciation run) and Bank Accounting (statement import + reconciliation) once AP/AR are stable.

**Non-goals**
- No tax determination (handled in Stage 3).
- No advanced workflows (approvals/parking belong to Stage 5, though APIs should be workflow-ready).
- No analytics/reporting beyond open-item dashboards (Stage 4).

---

## 2. Architecture & Data Model Deliverables

| Item | Description | Owners |
|------|-------------|--------|
| Vendor master schema (`vendors` table + contacts) | Shared across AP/AR with tenant + dimension defaults | Sub-ledger squad |
| Customer master schema (`customers`) | Same structure as vendor with credit limit, payment terms | Sub-ledger squad |
| AP document tables (`ap_invoices`, `ap_payments`, `ap_open_items`) | Capture header, line details, tax placeholders | AP team |
| AR document tables (`ar_invoices`, `ar_receipts`, `ar_open_items`) | Mirror AP tables with customer focus | AR team |
| Ledger extension columns | `subledger_type`, `subledger_document_id`, `dimension_snapshot` on journal lines | Finance Core |
| Asset Accounting tables | `asset_classes`, `assets`, `asset_postings`, `asset_depreciation_runs` | Asset team |
| Bank Accounting tables | `banks`, `bank_accounts`, `bank_statements`, `bank_reconciliation` | Bank team |

Notes:
- Use UUID primary keys, include `tenant_id`, `company_code_id`, and Stage 1 dimension foreign keys.
- All document tables include status, document number, posting date, due date, currency info, and audit metadata.

---

## 3. Service & API Work

1. **Master Data APIs**
   - `/api/v1/finance/vendors` and `/api/v1/finance/customers` with CRUD + search.
   - Batch import endpoints using CSV with validation (reuses Stage 1 validation rules).

2. **AP Service**
   - `/api/v1/finance/ap/invoices` (create, update, post, cancel).
   - `/api/v1/finance/ap/invoices/{id}/payments` to apply payments, handle partial/over payments.
   - Payment proposal generator + export for treasury teams.
   - Dunning scaffold (store levels, emit events to workflow engine later).

3. **AR Service**
   - `/api/v1/finance/ar/invoices` and `/api/v1/finance/ar/invoices/{id}/receipts`.
   - Credit memo + write-off commands with validation.
   - Open-item aging endpoints grouped by dimension, customer, and days past due.

4. **Asset Accounting**
   - `/api/v1/finance/assets` for lifecycle (create, capitalize, retire).
   - Depreciation run command + scheduler (monthly) producing GL postings referencing asset IDs.

5. **Bank Accounting**
   - `/api/v1/finance/bank/statements` ingestion API (CSV/MT940).
   - Reconciliation API to match statements with GL cash accounts; flag exceptions.

6. **Eventing**
   - Topics: `finance.ap.invoices.v1`, `finance.ar.invoices.v1`, `finance.asset.events.v1`, `finance.bank.events.v1`.
   - Outbox entries enriched with sub-ledger document identifiers and dimension snapshots.

_API examples live in [`docs/rest/finance-ap-ar.rest`](./rest/finance-ap-ar.rest) for quick smoke/manual testing._

---

## 4. Process & Posting Flow

1. AP invoice captured -> validated -> persisted -> posts to GL with dimension data -> open-item created -> event sent.
2. Payment run selects open items -> posts payment journal -> closes open items.
3. AR invoice -> GL posting -> open-item -> receipts close open item -> revenue recognition tracked.
4. Depreciation run calculates expense + accumulated depreciation -> GL posting referencing asset id.
5. Bank statements imported -> auto-match vs. GL -> unmatched lines flagged for manual reconciliation.

Ensure every flow is idempotent (keys per document) and uses Stage 1 dimension enforcement.

---

## 5. Observability & Tooling

- Metrics: `ap.invoice.posted`, `ar.invoice.posted`, `subledger.gl.sync.duration`, `asset.depreciation.run.duration`, `bank.reconciliation.coverage`.
- Dashboards: Open-item aging, payment backlog, reconciliation coverage, depreciation success.
- Logs: Structured logs per document with correlation ids; include `subledger_type` tag for search.
- Traces: Spans for invoice posting connecting API -> service -> GL command -> outbox publish.

---

## 6. Testing Strategy

| Layer | Tests |
|-------|-------|
| Unit | Domain aggregates (invoice lifecycle, payment matching, depreciation schedule). |
| Integration | Repository tests for master data + document persistence with optimistic locking. |
| Contract | REST + event schema tests for AP/AR/Asset/Bank services. |
| End-to-end | Testcontainers scenario: create vendor, AP invoice, payment, verify GL lines + events; same for AR, asset depreciation, bank reconciliation. |

Scenario additions to `docs/FINANCE_LIVE_TEST_GUIDE.md`:
- AP invoice from creation through payment reconciliation.
- AR invoice through receipt + dunning trigger.
- Asset depreciation run with GL verification.
- Bank statement import with auto-matching results.

---

## 7. Definition of Ready / Done

**Ready**
- Stage 1 dimensions GA and feature flag for sub-ledger enforcement toggled in lower envs.
- Master data ERDs approved; API specs stubbed in `docs/rest/finance-ap.ar.rest`.
- Kafka topics reserved in schema registry.

**Done**
- AP/AR services deployed with full CRUD/posting flows and automated tests.
- Asset + Bank MVP flows live, emitting events and GL postings.
- Ledger schema migrations applied; new columns populated for historical records (via backfill job).
- Open-item aging dashboards accurate within 5 minutes.
- Documentation updated: this plan, REST specs, runbooks, ArchUnit rules.

---

## 8. Timeline & Ownership

| Week | Focus | Exit Criteria |
|------|-------|---------------|
| Week 1 | Master data schemas + APIs | Vendor/customer APIs merged, migrations applied |
| Week 2 | AP workflows + GL integration | AP invoices post to GL, open-item tables populated |
| Week 3 | AR workflows + GL integration | AR invoices + receipts live, dunning scaffold ready |
| Week 4 | Asset depreciation + bank reconciliation | Depreciation run + bank import working in staging |
| Week 5 | Hardening + backfills | Ledger references backfilled, dashboards verified |

Owners: Sub-ledger squad (core), Asset specialist, Bank specialist, Finance Core for GL integration, QA for e2e tests.

---

## 9. Cutover Checklist

1. Migrate existing vendor/customer data or seed templates for demo tenants.
2. Backfill `subledger_type` and `subledger_document_id` for historical postings (default to `GL` when unknown).
3. Rehearse AP/AR workflows in staging with real integration points (payment provider mocks).
4. Enable sub-ledger enforcement flag per tenant once training complete.
5. Update change log + customer communication with new API endpoints and feature toggles.

Stage 2 completion unlocks Stage 3 tax hooks and ensures the finance stack now mirrors SAP's sub-ledger rigor.
