# Finance Stage 3 Execution Plan - Tax & Valuation Services

_Last updated: 2025-11-14_  
_Scope window: Phase 5C (Sprints 4-5)_  
_Source blueprint:_ `docs/FINANCE_SAP_PARITY_EXECUTION.md#4-stage-3-tax-valuation-services`

Stage 3 adds SAP-grade tax determination, valuation adjustments, and intercompany reconciliation so compliance and consolidation requirements are met.

---

## 1. Goals & Non-Goals

**Goals**
- Deliver a configurable tax engine that can determine jurisdiction, rate type, exemptions, and rounding for AP/AR documents.
- Automate deferred/unrealized tax postings leveraging the multi-currency foundation.
- Implement intercompany reconciliation (ICR) flows with discrepancy management.
- Provide audit-grade logs and metrics for compliance teams.

**Non-goals**
- No UI workflow for approvals (that remains Stage 5).
- No analytics dashboards beyond those needed for compliance KPIs (Stage 4 handles financial statement reporting).
- No legislative content packs (initial scope uses uploaded rate tables; localization packages tracked separately).

---

## 2. Architecture & Data Model Deliverables

| Item | Description | Owners |
|------|-------------|--------|
| Tax engine schema (`tax_jurisdictions`, `tax_rates`, `tax_rules`, `tax_codes`) | Stores jurisdiction hierarchy, rate types (spot, average, custom), effective dates | Compliance squad |
| Tax determination cache | Keyed by combination of company code, jurisdiction, product/service code, tax category | Compliance squad |
| Deferred tax tables (`tax_deferred_entries`, `tax_revaluation_runs`) | Track unrealized gains/losses adjustments | Finance Core |
| Intercompany reconciliation tables (`icr_pairs`, `icr_discrepancies`, `icr_runs`) | Store entity pairs, matching status, adjustments | Compliance squad |

---

## 3. Services & APIs

1. **Tax Engine Service**
   - API: `/api/v1/finance/tax/determine` (sync) and `/api/v1/finance/tax/rates` for CRUD.
   - gRPC endpoint for low-latency lookups from AP/AR services.
   - Supports rate selection (standard, reduced, zero), exemptions, reverse charge flags.
   - Returns tax breakdown per line item plus audit reference.

2. **Integration with AP/AR**
   - AP/AR invoicing must call tax engine before posting; store applied tax code/rate snapshot on invoice lines.
   - Emit `TaxApplied` events to `finance.tax.events.v1`.

3. **Deferred Tax & Valuation**
   - Scheduled job (monthly) revalues outstanding tax balances vs. current rates; posts GL adjustments.
   - Hooks into existing multi-currency revaluation for FX differences.

4. **Intercompany Reconciliation Service**
   - `/api/v1/finance/icr/runs` to start reconciliation for selected company code pairs.
   - Matching logic compares AP vs. AR documents between entities, flags deltas, produces adjustment proposals.
   - SLA metrics recorded for completion time and unresolved items.

5. **Compliance Dashboards**
   - REST endpoints to fetch tax exposure, deferred tax balances, ICR status for auditors.

---

## 4. Observability & Compliance Logging

- Metrics: `tax.determination.latency`, `tax.determination.cache.hit`, `tax.applied.count`, `icr.run.duration`, `icr.discrepancies.open`.
- Logs: Structured, tamper-evident logs for tax decisions (include jurisdiction, rule id, caller, correlation id).
- Audit trail: Persist audit events in append-only table with hash chaining for SOX readiness.
- Tracing: Add spans around AP/AR -> tax engine calls and ICR runs.

---

## 5. Testing Strategy

| Layer | Tests |
|-------|-------|
| Unit | Tax rule evaluation, jurisdiction selection, ICR matching heuristics. |
| Integration | AP/AR calling tax engine; deferred tax postings verifying GL entries. |
| Contract | REST/gRPC contract tests; schema registry validation for `finance.tax.events.v1`. |
| Compliance | Replay historical tax datasets to ensure calculations match reference numbers. |

Update `docs/FINANCE_LIVE_TEST_GUIDE.md` with scenarios:
- Multi-jurisdiction invoice (split tax codes).
- Deferred tax revaluation run.
- ICR run catching mismatched AP/AR invoices.

---

## 6. Definition of Ready / Done

**Ready**
- Stage 2 sub-ledger events emitting required metadata (jurisdiction, company code).
- Tax rules catalog defined for pilot tenants.
- Security review of tax engine service completed.

**Done**
- Tax engine deployed with rate CRUD + determination API, cached for <20ms p95.
- AP/AR integrated with tax engine; invoices fail if tax cannot be determined.
- Deferred tax job and intercompany reconciliation service running in staging with alerts.
- Compliance dashboards live; audit logs validated by internal audit.
- Documentation updated: this plan, ADRs, REST specs, runbooks.

---

## 7. Timeline & Ownership

| Week | Focus | Exit Criteria |
|------|-------|---------------|
| Week 1 | Tax schema + API contracts | Rate/jurisdiction migrations ready, OpenAPI published |
| Week 2 | Tax engine implementation + caching | Determination API returning values, cache hits logged |
| Week 3 | AP/AR integration, events, deferred tax job | Invoices enriched with tax snapshot, GL adjustments posting |
| Week 4 | Intercompany reconciliation service + dashboards | ICR runs complete in staging, dashboards display KPIs |
| Week 5 | Compliance validation + hardening | Audit sign-off, performance benchmarks met |

Owners: Compliance & Valuation squad, Finance Core (GL/deferred tax), DevEx (observability), QA/compliance testers.

---

## 8. Cutover Checklist

1. Import tax rules for pilot tenants (CSV or API).
2. Validate tax engine performance with load tests (10k determinations/min).
3. Run backfill to attach tax codes to existing AP/AR documents (where possible).
4. Execute dry-run ICR using historical data; resolve blockers.
5. Communicate tax rollout plan + required configuration to customers.

Completed Stage 3 unlocks Stage 4 reporting (which depends on tax accuracy) and closes major compliance gaps vs. SAP.

