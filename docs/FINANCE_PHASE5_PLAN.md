# Finance Phase 5 Expansion Plan: Enterprise-Grade Financial Management

_Last updated: 2025-11-14_  
_Status: Planning & Design Complete_  
_Target: SAP-Grade Feature Parity with SMB-to-Enterprise Scalability_

## Executive Summary

Phase 4 delivered the foundational financial-accounting slice with basic ledger, chart of accounts, accounting periods, and journal entry posting. **Phase 5 transforms this foundation into a comprehensive, enterprise-grade financial management system** that serves businesses from SMBs (single entity, simple accounting) through large enterprises (100+ entities, multi-currency, complex consolidation).

**Key Objectives:**
1. Achieve **SAP-grade feature parity** while maintaining cost advantage (avoid $50K-$500K/year licensing)
2. Support **all business sizes** through configuration-based feature enablement
3. Meet **regulatory compliance** requirements (SOX, GAAP, IFRS, tax jurisdictions)
4. Deliver **world-class performance** (journal post <200ms p95, consolidation <15 min for 50 entities)
5. Enable **seamless integrations** with 20+ internal contexts and external systems

**Investment:** 6-8 engineer-months over 4 sprints  
**Expected ROI:** $500K-$2M annual savings vs. SAP/Oracle licensing for 100+ customers

Phase 4 delivered the transactional outbox, schema registration, and gateway wiring for the financial-accounting slice. Phase 5 focuses on the features and controls required to approach "world-class SAP grade" parity. This document tracks the targeted capabilities and the associated ADR references.

> **Execution Blueprint:** The detailed stage-by-stage order for closing SAP-grade gaps now lives in `docs/FINANCE_SAP_PARITY_EXECUTION.md`. Use that blueprint to sequence delivery work while this plan tracks scope, sizing, and sprint allocations.

---

## Phase 5A Progress (Sprint 2 - In Progress)

**Sprint Goal:** Deliver mid-market feature pack (multi-currency, cost centers, reporting APIs, approval workflows)

**✅ Completed:**
1. **Sprint 0 Stabilization** (Phase 4 wrap-up):
   - ✅ Shared Money value object implementation (replaced financial-shared placeholders)
   - ✅ Micrometer instrumentation (@Timed, @Counted, gauges) on all command handlers
   - ✅ Readiness health checks (DB connectivity) at `/q/health/ready`
   - ✅ Audit metadata columns (created_by, updated_by, source_system) with JPA lifecycle hooks
   - ✅ REST endpoint documentation at `docs/rest/finance-accounting.rest`

2. **Multi-Currency Foundation** (Phase 5A.1):
   - ✅ Exchange rate database schema (V005__create_exchange_rates.sql)
   - ✅ ExchangeRateProvider interface + DatabaseExchangeRateProvider implementation
   - ✅ Foreign currency journal line support (V006__add_original_currency_to_journal_lines.sql)
   - ✅ Automatic conversion to base currency at posting time
   - ✅ `RunCurrencyRevaluationCommand` for unrealized gain/loss adjustments
   - ✅ `POST /api/v1/finance/ledgers/{ledgerId}/periods/{periodId}/currency-revaluation` REST endpoint
   - ✅ Unit and integration tests for revaluation logic
   - ✅ Micrometer instrumentation for revaluation operations

**🟡 In Progress:**
- None (multi-currency complete, ready for next feature)

**📋 Next Up (Priority Order):**
1. **Cost Centers & Dimensions** (Phase 5A.2) - Add cost_center_id, project_id, department_id to journal lines
2. **Query/Reporting APIs** (Phase 5A.3) - Trial balance, GL summary, period status endpoints
3. **Approval Workflow MVP** (Phase 5A.4) - Draft/submitted/approved/rejected states with audit logging

**Blockers:** None

**Estimated Completion:** Sprint 2 targeting 80% completion of mid-market tier features

---

## 1. Feature Roadmap by Business Tier

### 1.1 SMB Tier Features (Phase 4 - Complete ✅)
**Target:** 1-50 employees, single entity, basic accounting | **Time to Value:** 2-4 weeks | **Cost:** $100-500/month

| Feature | Status | API Endpoints | Notes |
|---------|--------|---------------|-------|
| Basic Ledger | ✅ Complete | `POST /api/v1/finance/ledgers` | Single ledger, base currency only |
| Chart of Accounts | ✅ Complete | `POST /api/v1/finance/chart-of-accounts/{id}/accounts` | Hierarchical, 5 account types |
| Journal Entries | ✅ Complete | `POST /api/v1/finance/journal-entries` | Manual posting, double-entry validation |
| Accounting Periods | ✅ Complete | `POST /api/v1/finance/periods/{id}/close` | Open/freeze/close workflow |
| Event Publishing | ✅ Complete | Kafka outbox pattern | `finance.journal.events.v1`, `finance.period.events.v1` |

---

### 1.2 Mid-Market Tier Features (Phase 5A - Sprint 2-3)
**Target:** 50-500 employees, 2-10 entities | **Time to Value:** 2-3 months | **Cost:** $500-2000/month

| Feature | Priority | Sprint | Complexity | Status |
|---------|----------|--------|------------|--------|
| **Multi-Currency Support** | P0 | Sprint 2 | High | ✅ Complete (conversion + revaluation + REST API) |
| **Cost Centers & Dimensions** | P0 | Sprint 2 | Medium | 🟡 Next |
| **Projects/Departments** | P1 | Sprint 2 | Medium | 🟡 Planned |
| **Query/Reporting APIs** | P0 | Sprint 2 | Medium | 🟡 Planned |
| **Tax Code Framework** | P0 | Sprint 2 | Medium | 🟡 Planned |
| **Basic Approval Workflows** | P0 | Sprint 3 | High | 🟡 Planned |
| **Intercompany Transactions** | P1 | Sprint 3 | High | 🟡 Planned |
| **Bank Reconciliation** | P1 | Sprint 3 | High | 🟡 Planned |
| **Batch Import/Export** | P2 | Sprint 3 | Low | 🟡 Planned |

**Detailed Specifications:**

#### 1.2.1 Multi-Currency Accounting ✅
- **Exchange Rate Management**: ✅ Database storage (`exchange_rates` table), ExchangeRateProvider interface, DatabaseExchangeRateProvider implementation
- **Foreign Currency Transactions**: ✅ Journal lines support `original_currency` and `original_amount`, auto-convert to ledger base currency at posting time
- **Realized Gains/Losses**: 🟡 Planned (calculated on payment/receipt against original booking rate)
- **Unrealized Gains/Losses**: ✅ `RunCurrencyRevaluationCommand` aggregates foreign exposures, posts balancing entries using gain/loss accounts
- **API Endpoints**: 
  - ✅ `POST /api/v1/finance/ledgers/{ledgerId}/periods/{periodId}/currency-revaluation` - Run revaluation job
  - 🟡 `POST /api/v1/finance/exchange-rates` - Create/update rates (pending)
  - 🟡 `GET /api/v1/finance/exchange-rates/convert` - Currency conversion calculator (pending)
- **Implementation Details:**
  - Migration V005: Created `exchange_rates` table with `from_currency`, `to_currency`, `rate`, `effective_from`, `effective_to`
  - Migration V006: Added `original_currency` and `original_amount` columns to `journal_entry_lines`
  - `AccountingCommandHandler.runCurrencyRevaluation()`: Aggregates posted non-base-currency lines, revalues at latest rate, posts adjustment entries
  - `JpaJournalEntryRepository.findPostedByLedgerAndPeriod()`: Query method for revaluation exposures
  - `FinanceCommandService`: Wraps revaluation with `@Timed("finance.revaluation")` and `@Counted`
  - REST endpoint: `POST /api/v1/finance/ledgers/{ledgerId}/periods/{periodId}/currency-revaluation` with `RunCurrencyRevaluationRequest` DTO
  - Tests: Unit tests in `AccountingCommandHandlerTest` and integration tests in `FinanceCommandServiceTest` + `FinanceCommandResourceTest`
- **Remaining Work:** 
  - Scheduler for monthly/quarterly automatic revaluation
  - Realized gain/loss calculation tied to settlements (AP/AR integration)
  - Exchange rate management REST APIs for manual entry

#### 1.2.2 Cost Centers & Multi-Dimensional Accounting
- **Dimensions Supported**: Cost centers, profit centers, projects, departments, business units
- **Configuration**: Per-tenant enablement, mandatory/optional by account type
- **Reporting**: Profit/loss by dimension, cross-dimensional analysis
- **API Endpoints**:
  - `POST /api/v1/finance/cost-centers` - Create cost center
  - `GET /api/v1/finance/reporting/by-dimension?dimension=costCenter&id={id}` - Dimension reports

#### 1.2.3 Query & Reporting APIs
- **Trial Balance**: Summary by account with debits/credits, filterable by date range
- **General Ledger Report**: Detailed transaction listing with drill-down
- **Aged Receivables/Payables**: Aging buckets (current, 30, 60, 90+ days)
- **Financial Statements**: Balance sheet, P&L, cash flow (basic templates)
- **Pagination**: Cursor-based pagination for large datasets (10K+ records)
- **Export Formats**: CSV, Excel, PDF, JSON

---

### 1.3 Enterprise Tier Features (Phase 5B - Sprint 3-4)
**Target:** 500+ employees, 10-100+ entities | **Time to Value:** 6-12 months | **Cost:** $2000-10000/month

| Feature | Priority | Sprint | Complexity | Status |
|---------|----------|--------|------------|--------|
| **Financial Consolidation** | P0 | Sprint 3 | Very High | 🟡 Planned |
| **Multi-Level Approvals** | P0 | Sprint 3 | High | 🟡 Planned |
| **Unrealized Gains/Losses** | P1 | Sprint 3 | High | 🟡 Planned |
| **Regulatory Reporting (GAAP/IFRS)** | P0 | Sprint 4 | Very High | 🟡 Planned |
| **SOX Controls & Segregation of Duties** | P0 | Sprint 4 | High | 🟡 Planned |
| **Advanced Tax Engine** | P1 | Sprint 4 | High | 🟡 Planned |
| **Cash Flow Forecasting** | P2 | Sprint 4 | Medium | 🟡 Planned |
| **Fixed Asset Integration** | P1 | Sprint 4 | Medium | 🟡 Planned |
| **Budget Management** | P2 | Sprint 4 | Medium | 🟡 Planned |
| **Audit Trail & Forensics** | P0 | Sprint 4 | High | 🟡 Planned |

**Detailed Specifications:**

#### 1.3.1 Financial Consolidation
- **Currency Translation**: Balance sheet (closing rates), P&L (average rates), equity (historical rates)
- **Intercompany Elimination**: Auto-match and eliminate payables/receivables, sales/purchases
- **Minority Interests**: Calculate non-controlling interest for partial ownership
- **Consolidation Reports**: Consolidated balance sheet, P&L, cash flow with drill-down
- **Performance**: <15 minutes for 50 entities with 1M transactions
- **API Endpoints**:
  - `POST /api/v1/finance/consolidation/execute` - Run consolidation
  - `GET /api/v1/finance/consolidation/{id}/report` - Consolidated financials

#### 1.3.2 SOX Compliance & Controls
- **Segregation of Duties**: Separate roles for journal entry vs. approval vs. posting
- **Access Reviews**: Automated quarterly reports of user permissions
- **Change Tracking**: Immutable audit log for all posted transactions
- **Control Testing**: Automated tests for key controls (e.g., 3-way matching)
- **Audit Reports**: Pre-built reports for external auditors

#### 1.3.3 Regulatory Reporting
- **Standards Supported**: US GAAP, IFRS, UK GAAP, German HGB, French PCG
- **Report Templates**: 
  - Balance Sheet (Statement of Financial Position)
  - Income Statement (P&L)
  - Statement of Cash Flows (Direct & Indirect methods)
  - Statement of Changes in Equity
  - Notes to Financial Statements (configurable templates)
- **XBRL Export**: Support for electronic filing requirements
- **Audit Trail**: Complete change history with cryptographic signatures

---

## 2. Operational & Observability Enhancements

| Area | Planned Work | Notes |
|------|--------------|-------|
| **SLIs / Metrics** | Emit business metrics: journal throughput, approval latency, period-close duration, outbox lag. Wire dashboards + alerts. | Extend `FinanceOutboxEventScheduler` metrics and add new Micrometer timers. |
| **Load & chaos testing** | Create k6/JMeter suites targeting 200 ms p95 for journal posts and 300 ms for ledger creation. Introduce chaos experiments for DB + Kafka outages. | Document procedures in `docs/PERF_TEST_PLAN.md` (future). |
| **Security scopes** | Enforce financial roles (`financial-admin`, `financial-user`, `financial-auditor`) at the gateway + service layers. Add audit logging for privileged actions. | Update API Gateway config + service authorization interceptors. |
| **Runbooks** | Produce deploy/troubleshoot runbooks covering migrations, ledger reconciliation, and schema registry operations. | Link from `docs/runbooks/FINANCE_OPERATIONS.md` (planned). |

---

## 3. Timeline & Dependencies

1. **Sprint 1 (current)** – Finalize planning, scaffold ExchangeRate policy, document schema registry workflow. ✅
2. **Sprint 2** – Implement multi-currency foundation (domain + persistence) and trial balance query APIs.
3. **Sprint 3** – Introduce approval workflow, cost-center tagging, and initial dashboards.
4. **Sprint 4** – Load testing, chaos drills, and runbooks for GA readiness.

Dependencies: schema registry, identity scopes, Redpanda/Postgres observability stack, SAP parity requirements from SME workshops.

---

## 4. Configuration-Based Feature Enablement

### 4.1 Feature Tiers Configuration

Features are enabled via tenant configuration stored in database:

```yaml
# Example tenant configuration
tenant_id: "550e8400-e29b-41d4-a716-446655440000"
financial_tier: "ENTERPRISE"  # Options: SMB, MIDMARKET, ENTERPRISE

features:
  # Core (always enabled)
  basic_ledger: true
  chart_of_accounts: true
  journal_entries: true
  accounting_periods: true
  
  # Mid-Market tier
  multi_currency: true
  cost_centers: true
  projects: true
  departments: true
  basic_approvals: true
  intercompany_tracking: true
  bank_reconciliation: true
  tax_codes: true
  
  # Enterprise tier
  profit_centers: true
  business_units: true
  multi_level_approvals: true
  consolidation: true
  unrealized_gains: true
  sox_controls: true
  regulatory_reporting: true
  advanced_tax_engine: true
  cash_flow_forecasting: false  # Optional even for enterprise
  budget_management: false
```

### 4.2 Feature Dependencies & Constraints

| Feature | Depends On | Incompatible With | Configuration Validation |
|---------|------------|-------------------|--------------------------|
| Multi-Currency | - | - | Requires base currency on all ledgers |
| Unrealized Gains | Multi-Currency | - | Requires gain/loss accounts in COA |
| Consolidation | Multi-Currency, Intercompany | - | Minimum 2 entities required |
| Multi-Level Approvals | Basic Approvals | - | Requires at least 2 approval levels |
| SOX Controls | Multi-Level Approvals | - | Enforces segregation of duties |
| Regulatory Reporting | Consolidation (optional) | - | Requires accounting standard selection |

### 4.3 Migration Paths Between Tiers

**SMB → Mid-Market Upgrade:**
1. Enable multi-currency feature flag
2. Run migration to add foreign currency columns
3. Import initial exchange rates
4. Configure cost center hierarchy
5. Define approval workflow rules
6. User training (estimated 1-2 weeks)

**Mid-Market → Enterprise Upgrade:**
1. Enable consolidation feature flag
2. Configure intercompany relationships
3. Set up elimination rules
4. Define regulatory reporting templates
5. Implement SOX controls and role separation
6. User training and process documentation (estimated 1-3 months)

**Downgrade Path:**
- **Not Supported**: Cannot downgrade tiers due to data model dependencies
- **Alternative**: Export data, provision new tenant at lower tier, import compatible subset

---

## 5. Integration Architecture

### 5.1 Upstream Systems (Event Consumers)

| Context | Events Consumed | Journal Generation Logic |
|---------|-----------------|--------------------------|
| **Accounts Payable** | `ap.invoice.approved.v1` | DR: Expense/Asset account, CR: AP control account |
| **Accounts Receivable** | `ar.invoice.issued.v1` | DR: AR control account, CR: Revenue account |
| **Payroll** | `payroll.run.completed.v1` | DR: Salary expense accounts, CR: Payroll liability |
| **Inventory** | `inventory.valuation.adjusted.v1` | DR/CR: Inventory account, CR/DR: COGS |
| **Fixed Assets** | `assets.depreciation.calculated.v1` | DR: Depreciation expense, CR: Accumulated depreciation |
| **Treasury** | `treasury.payment.executed.v1` | DR: AP/liability, CR: Cash/bank account |
| **Commerce** | `commerce.order.fulfilled.v1` | DR: COGS, CR: Inventory, DR: AR, CR: Revenue |
| **Procurement** | `procurement.receipt.confirmed.v1` | DR: Inventory/expense, CR: AP accrual |

### 5.2 Downstream Systems (Event Publishers)

| Event | Consumers | Use Case |
|-------|-----------|----------|
| `finance.journal.posted.v1` | BI, External ERP, Audit Systems | Real-time financial analytics, data lake sync |
| `finance.period.closed.v1` | Reporting, Compliance, BI | Trigger monthly reports, lock transactions |
| `finance.consolidation.completed.v1` | Executive Dashboards, Board Reporting | Consolidated financial statements |
| `finance.approval.workflow.v1` | Notification Service, Workflow Engine | Email/SMS notifications to approvers |
| `finance.bank.reconciliation.completed.v1` | Treasury, Cash Management | Cash position updates |
| `finance.tax.calculated.v1` | Tax Filing Systems, Compliance | Tax return preparation |
| `finance.currency.revaluation.v1` | Treasury, Risk Management | FX exposure reporting |

### 5.3 External System Integrations

| System Type | Integration Method | Data Flow | Frequency |
|-------------|-------------------|-----------|-----------|
| **Banks** | SFTP/API (MT940, BAI2) | Import statements | Daily |
| **Tax Engines** | REST API (Avalara, Vertex) | Calculate tax amounts | Real-time |
| **Payment Processors** | Webhook + API | Payment confirmation | Real-time |
| **External ERPs** | File Export/Import | Journal entries, trial balance | Daily/Weekly |
| **Audit Tools** | Data Export API | Complete audit trail | On-demand |
| **BI/Analytics** | Event Streaming | Real-time financial data | Continuous |
| **Exchange Rate Providers** | REST API (ECB, Bloomberg) | Daily rate updates | Daily 00:00 UTC |

---

## 6. Testing Strategy

### 6.1 Test Coverage Targets

| Layer | Unit Tests | Integration Tests | Contract Tests | E2E Tests |
|-------|------------|-------------------|----------------|-----------|
| Domain | 95% coverage | N/A | N/A | N/A |
| Application | 90% coverage | Repository tests | N/A | N/A |
| Infrastructure | 80% coverage | REST API tests | Event schema tests | Full workflow tests |
| **Overall Target** | **90%** | **All critical paths** | **All events** | **Happy path + 3 error scenarios** |

### 6.2 Financial-Specific Test Scenarios

**Double-Entry Validation:**
- Balanced journal entries (debits = credits)
- Unbalanced entries rejected with clear error messages
- Currency consistency within entry
- Account type validation (no DR to revenue without explanation)

**Multi-Currency Tests:**
- Foreign currency posting with auto-conversion
- Exchange rate lookup (historical and current)
- Realized gain/loss on payment
- Unrealized gain/loss calculation
- Currency revaluation accuracy

**Period Close Tests:**
- Cannot post to closed period
- Can reopen period with proper authorization
- All journals must be approved before close
- Trial balance balanced after close

**Consolidation Tests:**
- Currency translation accuracy (BS vs P&L rates)
- Intercompany elimination completeness
- Minority interest calculation
- Drill-down from consolidated to entity level

**Approval Workflow Tests:**
- Single approval flow
- Multi-level approval with escalation
- Rejection and resubmission
- Delegation during approval absence
- Timeout and SLA alerts

---

## 7. References & Related Documentation

### 7.1 Architecture Decision Records
- **ADR-001** – Modular CQRS Implementation
- **ADR-002** – Database Per Bounded Context (Schema-per-context)
- **ADR-003** – Event-Driven Integration Between Contexts
- **ADR-005** – Multi-Tenancy Data Isolation Strategy
- **ADR-006** – Platform-Shared Governance Rules
- **ADR-007** – Authentication & Authorization Strategy
- **ADR-008** – CI/CD Pipeline Architecture & Network Resilience
- **ADR-009** – Financial Accounting Domain Strategy (✅ Updated for Phase 5)

### 7.2 Platform Documentation
- `docs/ARCHITECTURE.md` – Overall system architecture
- `docs/ROADMAP.md` – Implementation phases and milestones
- `docs/PHASE4_READINESS.md` – Phase 4 completion criteria
- `docs/OBSERVABILITY_BASELINE.md` – Metrics and monitoring standards
- `docs/SECURITY_SLA.md` – Security SLAs and compliance requirements
- `docs/EVENT_VERSIONING_POLICY.md` – Event schema versioning rules
- `docs/SCHEMA_REGISTRY_STATUS.md` – Schema registry operations

### 7.3 Financial Context Documentation
- `bounded-contexts/financial-management/README.md` – Context overview
- `bounded-contexts/financial-management/financial-accounting/README.md` – Accounting module details
- `docs/schemas/finance/` – Event schema definitions

### 7.4 Operational Documentation (Planned)
- `docs/runbooks/FINANCE_DEPLOYMENT.md` – Deployment procedures
- `docs/runbooks/FINANCE_PERIOD_CLOSE.md` – Period close checklist
- `docs/runbooks/FINANCE_TROUBLESHOOTING.md` – Issue resolution guide
- `docs/runbooks/FINANCE_DISASTER_RECOVERY.md` – DR procedures
- `docs/runbooks/FINANCE_CONSOLIDATION_OPS.md` – Consolidation operations

---

## 8. Success Criteria & Acceptance

### 8.1 Phase 5A Completion (Mid-Market Features)
- ✅ Multi-currency support with 10+ currencies configured
- ✅ Cost center hierarchy with 50+ cost centers
- ✅ Query APIs returning trial balance in <2s for 100K transactions
- ✅ Basic approval workflow with 2-level approval demonstrated
- ✅ Intercompany transactions tracked across 3+ entities
- ✅ Bank reconciliation matching 95%+ of transactions automatically
- ✅ Load tests passing: 100 journals/second sustained
- ✅ All unit tests passing with 90%+ coverage
- ✅ API documentation complete with .rest files

### 8.2 Phase 5B Completion (Enterprise Features)
- ✅ Financial consolidation for 10+ entities in <10 minutes
- ✅ Multi-level approval workflows (3+ levels) operational
- ✅ Unrealized gain/loss revaluation running monthly
- ✅ Regulatory reports (GAAP/IFRS) generated successfully
- ✅ SOX controls enforced with segregation of duties
- ✅ Advanced tax engine integrated (Avalara or equivalent)
- ✅ Load tests passing: Consolidation of 50 entities <15 min
- ✅ Chaos tests demonstrating resilience to DB/Kafka failures
- ✅ Complete operational runbooks published

### 8.3 Business Value Metrics
- **Cost Savings**: $500K-$2M annually vs. SAP/Oracle licensing (100+ customers)
- **Implementation Speed**: Mid-market deployment in 2-3 months (vs. 6-12 months for SAP)
- **User Satisfaction**: NPS score >40 from finance teams
- **System Reliability**: 99.9% uptime SLA met
- **Performance**: All SLIs within target thresholds
- **Compliance**: Zero audit findings related to system controls

---

## 9. Current Implementation Status (Phase 4 Baseline)

### 9.1 Phase 4 Achievements ✅

**Domain Layer (accounting-domain):**
- ✅ Core aggregates implemented: Ledger, ChartOfAccounts, Account, AccountingPeriod, JournalEntry
- ✅ Value classes for type safety: LedgerId, AccountId, ChartOfAccountsId, AccountingPeriodId
- ✅ Money value object using BIGINT (stores minor currency units to avoid floating-point errors)
- ✅ Double-entry validation: Debits must equal credits
- ✅ Domain tests: JournalEntryTest, AccountingPeriodTest, ChartOfAccountsTest, MoneyTest (4 test classes)
- ✅ Immutable domain models with business logic protection

**Application Layer (accounting-application):**
- ✅ AccountingCommandHandler with CQRS command handlers
- ✅ Command DTOs: CreateLedgerCommand, DefineAccountCommand, PostJournalEntryCommand, CloseAccountingPeriodCommand
- ✅ Port/adapter interfaces: LedgerRepository, ChartOfAccountsRepository, AccountingPeriodRepository, JournalEntryRepository
- ✅ FinanceEventPublisher port for outbox pattern
- ✅ Application tests: AccountingCommandHandlerTest (1 test class)

**Infrastructure Layer (accounting-infrastructure):**
- ✅ REST API: FinanceCommandResource with `/api/v1/finance/**` endpoints
- ✅ JPA repositories: JpaLedgerRepository, JpaChartOfAccountsRepository, JpaAccountingPeriodRepository, JpaJournalEntryRepository
- ✅ JPA entities with @Version for optimistic locking
- ✅ Flyway migration: V001__create_financial_accounting_schema.sql (dedicated financial_accounting schema)
- ✅ Outbox pattern: FinanceOutboxEventEntity, FinanceOutboxRepository, FinanceOutboxEventScheduler
- ✅ Kafka publisher: KafkaFinanceEventPublisher emitting finance.journal.events.v1 and finance.period.events.v1
- ✅ DTO layer: CreateLedgerRequest, DefineAccountRequest, PostJournalEntryRequest, ClosePeriodRequest
- ✅ Integration tests: FinanceCommandResourceTest, FinanceCommandServiceTest, KafkaFinanceEventPublisherTest, FinanceOutboxIntegrationTest
- ✅ Test infrastructure: Testcontainers (PostgreSQL, Kafka) with @QuarkusTest
- ✅ Dependencies: Quarkus, PostgreSQL, Kafka, Micrometer, Flyway, Hibernate Validator

**Test Coverage:**
- ✅ 25 @Test methods across 11 test files
- ✅ Domain: 100% of core aggregates tested
- ✅ Application: Command handler logic tested with in-memory repositories
- ✅ Infrastructure: REST endpoints, JPA repositories, Kafka publishers tested
- ✅ 38 total Kotlin test files in financial-management context

### 9.1.1 Phase 5A Progress (In Flight)
- ✅ **Exchange rate persistence + policy** — Database-backed `JpaExchangeRateRepository` and `DatabaseExchangeRateProvider` serve FX lookups for posting and treasury workflows.
- ✅ **FX-aware postings** — `AccountingCommandHandler.postJournalEntry` converts mixed-currency lines into the ledger base currency while persisting `original_currency` / `original_amount` for every line (Flyway V006).
- ✅ **Unrealized gain/loss automation** — `RunCurrencyRevaluationCommand` (exposed via `FinanceCommandUseCase` and `FinanceCommandService`) posts balancing entries with configurable gain/loss accounts, emits `finance.command.fx_revaluation.*` Micrometer metrics, and publishes journal events.
- 🔜 **Remaining** — Realized gain/loss calculations on settlement, scheduler + REST/gateway endpoint for `/api/v1/finance/currency-revaluation`, idempotency controls, and tenant-level approval hooks.

### 9.2 Known Gaps & Technical Debt ⚠️

**Critical Gaps:**
1. **❌ Metrics Annotations**: No @Counted or @Timed on command handlers or REST endpoints (Micrometer dependency added but not used)
2. **❌ Health Checks**: quarkus-smallrye-health dependency added, but no custom FinanceHealthCheck implemented
3. **❌ API Gateway Integration**: REST endpoints exist but gateway proxy routes and scopes not configured
4. **❌ Query APIs**: No read-side endpoints for trial balance, ledger reports, financial statements
5. **❌ API Documentation**: No .rest files for Postman/HTTP Client testing; OpenAPI specs not generated

**Module Placeholders (Not Production-Ready):**
- **❌ Accounts Payable (financial-ap)**: All files are placeholder objects (BillPlaceholder, VendorPaymentPlaceholder, ThreeWayMatchingServicePlaceholder)
- **❌ Accounts Receivable (financial-ar)**: All files are placeholder objects (InvoiceResourcePlaceholder, OrderEventConsumerPlaceholder)
- **❌ Shared Financial Types (financial-shared)**: Money, Currency, FiscalPeriod are placeholder objects (duplicate Money exists in accounting-domain)

**Database Schema Gaps:**
- **⚠️ Audit Columns Missing**: created_by, modified_by not present (only created_at, updated_at timestamps)
- **⚠️ Foreign Currency Support**: No columns for foreign_currency, foreign_amount, exchange_rate on journal_entry_lines
- **⚠️ Dimensions Support**: No columns for cost_center_id, project_id, department_id on journal_entry_lines
- **⚠️ Approval Workflow**: No tables for approval_workflows, approval_steps, approval_history

**Observability Gaps:**
- No business metrics emitted (journal volume, approval latency, period close duration)
- No distributed tracing configured (OpenTelemetry dependency missing)
- No structured logging with correlation IDs (MDC not configured)
- No performance dashboards or Grafana panels defined
- No alert thresholds configured (p95 latency, error rate, outbox lag)

**Testing Gaps:**
- No contract tests for event schemas (finance.journal.events.v1, finance.period.events.v1)
- No load tests (k6/JMeter scripts for 100 journals/second)
- No chaos tests (database failover, Kafka outage, network partition)
- No end-to-end tests across multiple contexts
- No API integration tests with actual gateway

### 9.3 Phase 5 Prerequisites

**Sprint 0 (Technical Debt Cleanup - 1 week):**
1. Add @Timed("finance.journal.post") and @Counted annotations to all command handlers
2. Implement FinanceHealthCheck with database connectivity and Kafka producer checks
3. Generate .rest files for all `/api/v1/finance/**` endpoints
4. Add created_by, modified_by audit columns via new Flyway migration
5. Consolidate Money implementation (remove placeholder from financial-shared, reuse domain Money)
6. Configure API Gateway routes with financial-admin, financial-user, financial-auditor scopes
7. Add OpenTelemetry tracing with correlation ID propagation
8. Create Grafana dashboard for financial operations metrics

**Sprint 1 (Foundation for Phase 5 - 2 weeks):**
1. Implement query APIs: TrialBalanceQueryHandler, GeneralLedgerQueryHandler
2. Add read-side REST endpoints: `/api/v1/finance/reporting/trial-balance`, `/api/v1/finance/reporting/general-ledger`
3. Create contract tests for event schemas using Apicurio Schema Registry
4. Develop k6 load test scripts targeting 100 journals/second baseline
5. Write chaos test scenarios (DB failover, Kafka unavailable) using Chaos Mesh
6. Document API Gateway integration requirements
7. Add distributed tracing to all financial operations
8. Configure alert thresholds in Prometheus/Alertmanager

Only after Sprint 0 and Sprint 1 completion should Phase 5A (multi-currency, cost centers) begin.

### 9.4 Dependency on Other Contexts

**Blocked by:**
- **API Gateway**: Financial endpoints need gateway proxy configuration (in progress)
- **Tenancy & Identity**: financial-admin, financial-user, financial-auditor scopes need to be defined
- **Schema Registry**: Event schemas need to be registered before publishing to production Kafka

**Blocks:**
- **Accounts Payable**: Cannot implement AP until accounting foundation is complete and query APIs exist
- **Accounts Receivable**: Cannot implement AR until accounting foundation is complete and query APIs exist
- **Business Intelligence**: BI dashboards depend on finance.journal.events.v1 and finance.period.events.v1
- **Compliance/Audit**: External audit tools depend on complete audit trail and regulatory reporting

### 9.5 Risk Assessment

**High Risk:**
- **AP/AR Placeholder Status**: Entire AP and AR modules are stubs; scope creep risk if Phase 5 attempts to build these
- **Performance Unvalidated**: No load tests conducted; risk of not meeting <200ms p95 journal post target
- **Gateway Integration Unknown**: API endpoints may not integrate correctly with gateway auth/routing

**Medium Risk:**
- **Observability Gaps**: Limited visibility into system health could delay issue detection
- **No Chaos Testing**: Resilience to failures unproven; production incidents likely
- **Single Currency Only**: Cannot serve international customers without multi-currency support

**Low Risk:**
- **Core Domain Model**: Well-designed with proper DDD patterns and invariants
- **Test Coverage**: Good coverage at domain and integration levels
- **Infrastructure Choices**: Proven stack (Quarkus, PostgreSQL, Kafka)




