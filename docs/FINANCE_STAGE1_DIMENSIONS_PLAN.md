# Finance Stage 1 Execution Plan - Organizational Structure & Fiscal Dimensions

_Last updated: 2025-11-14_  
_Scope window: Phase 5A.2 (Sprints 2-3)_  
_Source blueprint:_ See `docs/FINANCE_SAP_PARITY_EXECUTION.md#2-stage-1-organizational-structure-fiscal-dimensions`

This plan expands Stage 1 into actionable work so engineering squads can start delivery immediately. Use it to seed Jira epics (`FIN-500` through `FIN-505`), architecture reviews, and Definition of Done checklists.

---

## 1. Goals & Non-Goals

**Goals**
- Provide SAP-grade entity hierarchy (company code abstraction, cost/profit centers, departments, business areas, projects).
- Introduce fiscal year variants and blackout enforcement so periods can follow calendar or 4-4-5 style patterns per entity.
- Ensure every journal entry captures the required dimensions and rejects invalid combinations before posting.
- Emit events and metrics so downstream services (reporting, sub-ledgers) can consume the new dimension metadata.

**Non-goals**
- No UI work beyond API/CLI stubs (portal updates tracked separately).
- No reporting changes beyond trial-balance slicing (full reporting suite is Stage 4).
- No sub-ledger logic; just hooks that later stages will rely on.

---

## 2. Architecture & Data Model Deliverables

| Item | Description | Owners |
|------|-------------|--------|
| `V007__finance_dimensions.sql` | Creates `cost_centers`, `profit_centers`, `departments`, `projects`, `business_areas` tables plus shared constraints | Data Engineering |
| `V008__company_code_structures.sql` | Adds `company_codes` table linked to tenants and legal entities | Data Engineering |
| `V009__fiscal_year_variants.sql` | Adds variants, period templates, blackout rules | Data Engineering |
| Dimension aggregates | Kotlin domain models + repositories with lifecycle invariants (status, validity dates, hierarchy links) | Finance Core squad |
| Assignment policies | Config tables + validation services determining which dimensions are mandatory per account type | Finance Core squad |

### ERD Notes
- All dimension tables include `tenant_id`, `code`, `name`, `status`, `valid_from`, `valid_to`, `parent_id` (for hierarchy when applicable), and auditing columns.
- Company codes map one-to-many with existing tenants so we can support multi-company per tenant.
- Fiscal year variants store the template in JSONB (ordered list of period lengths) plus metadata for start month and calendars.

---

## 3. Service & API Work

1. **Dimension Management API (`/api/v1/finance/dimensions/*`)**
   - CRUD endpoints per dimension type with optimistic locking.
   - Input validation: unique code per tenant, status transitions (draft -> active -> retired).
   - Bulk import endpoint for CSV upload (server-side validation only; full mass upload waits for Stage 5).

2. **Company Code & Fiscal Variant APIs**
   - `/api/v1/finance/company-codes` to create/assign company codes to tenants.
   - `/api/v1/finance/fiscal-year-variants` to create templates and assign to company codes.
   - Endpoint to roll periods based on variant selection (generates period records for upcoming fiscal year).
   - `/api/v1/finance/company-codes/{companyCodeId}/ledgers` to persist ledger mappings for downstream reporting.

3. **Journal Posting Enforcement**
   - Update command handlers so posting requires the dimensions flagged as mandatory for the ledger's account type.
   - Extend validation errors to include `missingDimensions` payload for UI guidance.

4. **Kafka Contracts**
   - Topic `finance.dimensions.events.v1` with events for `DimensionCreated`, `DimensionUpdated`, `DimensionRetired`.
   - Register schemas via existing registry playbook (`docs/SCHEMA_REGISTRY_STATUS.md`).

---

## 4. Observability & Tooling

- Micrometer: add `dimension_id`, `company_code`, and `fiscal_variant` tags to posting metrics and trial balance timers. Counters exposed today:
  - `finance.dimension.validation.failures` (tags: `reason`, `dimensionType`, `accountType`) – alerts on mandatory/invalid dimension usage.
  - `finance.dimension.orphan_lines` (tag: `accountType`) – surfaces postings that still bypass optional dimensions.
- Reporting SLA: dashboards track `finance.trialbalance.fetch` and `finance.glsummary.fetch` to keep p95 latency <300 ms for 10k posted lines. Alert if either exceeds SLA for 5 consecutive minutes.
- Dashboards: Grafana panel combining the two counters above with trial-balance latency (`finance.trialbalance.fetch`) and a table of dimension CRUD activity. Trigger pager alert when `validation.failures` > 0 for 5 minutes or orphan lines increase week over week.
- Logging: structured logs for dimension validation failures with correlation ids.

---

## 5. Testing Strategy

| Layer | Tests |
|-------|-------|
| Unit | Dimension aggregates (state transitions, hierarchy validation, assignment policy checks). |
| Integration | Repository tests verifying migrations, constraints, and optimistic locking. |
| Contract | REST docs / OpenAPI verification for new endpoints. |
| End-to-end | Trial balance query returning per-dimension slices; journal posting rejecting incomplete dimension payloads. |

Add scenarios to `docs/FINANCE_LIVE_TEST_GUIDE.md` covering:
- Creating a company code, fiscal variant, and generating periods.
- Posting to GL with missing cost center -> expect HTTP 400 with `missingDimensions`.
- Producing/consuming `finance.dimensions.events.v1`.

---

## 6. Definition of Ready / Done

**Ready**
- ERDs reviewed and signed off by data + domain leads.
- API contracts drafted in `docs/rest/finance-dimensions.rest`.
- Schema registry placeholders allocated for events.

**Done**
- All migrations applied in lower environments with rollback scripts.
- APIs + command handlers deployed and covered by automated tests.
- Metrics and dashboards live with alerts <5 min latency.
- Trial balance endpoint meets <300ms p95 when slicing by new dimensions (dataset: 10k journal lines).
- Documentation updated: this plan, REST specs, runbooks, quality gates.

---

## 7. Execution Timeline & Ownership

| Week | Focus | Exit Criteria |
|------|-------|---------------|
| Week 1 | Finalize ERDs, migrations, and API contracts | PRs merged for migrations + OpenAPI specs |
| Week 2 | Implement dimension management service + events | CRUD endpoints GA, events flowing in dev |
| Week 3 | Wire journal validation + trial balance slicing + dashboards | Posting enforcement live, dashboards alerting |
| Week 4 | Hardening, backfill scripts, cutover prep | Data migration scripts tested, documentation signed off |

Owners: Finance Platform Core squad (backend), Data Engineering (migrations), DevEx (observability), QA (scenarios).

---

## 8. Cutover & Backfill Checklist

1. Export existing tenant configuration and seed default company code + fiscal variant per tenant.
2. Provide script to backfill historical journal entries with default dimension values where mandatory (flag for manual review if unavailable).
3. Run dry-run migrations in staging, compare before/after row counts.
4. Enable new validation flags per tenant behind feature toggle (`finance.dimensions.enforced`).
5. Communicate release plan via `docs/FINANCE_LIVE_TEST_GUIDE.md` + platform changelog.

---

Once all checklist items are green, Stage 1 is ready to start implementation workstreams. Use this document as the authoritative reference during sprint planning and update it as architectural decisions evolve.


