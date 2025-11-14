
# ADR-009: Financial Accounting Domain Strategy

**Status:** Accepted  
**Date:** 2025-11-13  
**Last Updated:** 2025-11-14 (Phase 5 Enterprise Enhancements)  
**Context:** Phase 4 – First financial slice (`financial-management/financial-accounting`)  
**Scope:** SMB to Enterprise-grade financial management

## Decision
Establish `financial-accounting` as an autonomous bounded context responsible for comprehensive financial management including general ledger, chart of accounts, accounting periods, journal entry posting, multi-currency support, cost allocation, tax compliance, and regulatory reporting. The context will:

### Core Capabilities (Phase 4 - Foundation)
- Maintain domain aggregates (`Ledger`, `ChartOfAccounts`, `AccountingPeriod`, `JournalEntry`, `PostingBatch`) with strict double-entry invariants.
- Persist to a dedicated schema `financial_accounting` (per ADR-002) with Flyway-managed migrations.
- Expose synchronous APIs via the API Gateway under `/api/v1/finance/**` guarded by new `financial-*` scopes.
- Publish/consume versioned events (`finance.journal.events.v1`, `finance.period.events.v1`) registered in Schema Registry per the event-versioning policy.
- Reuse platform conventions (Quarkus stack, transactional outbox, Micrometer/OTel, shared JWT/tenant headers) to meet SAP-grade availability/SLA targets.

### Enterprise Capabilities (Phase 5 - Expansion)
- **Multi-currency accounting** with real-time exchange rates, currency translation, and unrealized gain/loss tracking
- **Multi-dimensional accounting** supporting cost centers, profit centers, projects, departments, business units
- **Tax compliance framework** with VAT/GST/Sales Tax engines, withholding tax, and reverse charge mechanisms
- **Approval workflows** with maker-checker patterns, multi-level authorization, and audit trails
- **Intercompany transactions** with automatic elimination entries and consolidation support
- **Regulatory reporting** supporting GAAP, IFRS, SOX compliance, and country-specific requirements
- **Bank reconciliation** with automatic matching, exception handling, and cash position management
- **Financial consolidation** with currency translation, elimination entries, and minority interests

### Core Aggregates & API Surfaces

#### Foundation Aggregates (Phase 4)
- **Ledger** – owns base currency + chart-of-accounts linkage; API: `POST /api/v1/finance/ledgers`, `GET /api/v1/finance/ledgers/{id}`.
- **ChartOfAccounts & Account** – hierarchical account catalog with account types (Asset, Liability, Equity, Revenue, Expense); API: `POST /api/v1/finance/chart-of-accounts/{id}/accounts`, `GET /api/v1/finance/chart-of-accounts/{id}`.
- **AccountingPeriod** – open/freeze/close workflow with validation gates; API: `POST /api/v1/finance/ledgers/{ledgerId}/periods`, `PUT /api/v1/finance/periods/{id}/close`.
- **JournalEntry / PostingBatch** – double-entry capture with balanced validation; API: `POST /api/v1/finance/journal-entries`, `POST /api/v1/finance/posting-batches/{id}/submit`.

#### Enterprise Aggregates (Phase 5)
- **ExchangeRate** – manages currency conversion rates with effective dating and rate types (spot, average, closing); API: `POST /api/v1/finance/exchange-rates`, `GET /api/v1/finance/exchange-rates/convert`.
- **CostCenter** – organizational units for management accounting; API: `POST /api/v1/finance/cost-centers`, `GET /api/v1/finance/cost-centers`.
- **TaxCode** – tax rate definitions with jurisdiction support; API: `POST /api/v1/finance/tax-codes`, `GET /api/v1/finance/tax-codes`.
- **ApprovalWorkflow** – multi-level authorization with delegation; API: `POST /api/v1/finance/workflows/{entityType}/{entityId}/approve`.
- **BankAccount** – bank account master data with reconciliation tracking; API: `POST /api/v1/finance/bank-accounts`, `GET /api/v1/finance/bank-accounts/{id}/statements`.
- **ConsolidationRule** – intercompany elimination and currency translation rules; API: `POST /api/v1/finance/consolidation/rules`, `POST /api/v1/finance/consolidation/execute`.

### Event Flows

#### Outbound Events
- **Core Events (Phase 4)**
  - `finance.journal.events.v1` – emitted after journal posting; consumed by BI, AP/AR, external ERPs.
  - `finance.period.events.v1` – broadcast on period freeze/close to trigger reporting.
  - `finance.reconciliation.events.v1` – optional for downstream compliance engines.

- **Enterprise Events (Phase 5)**
  - `finance.currency.translation.events.v1` – currency revaluation completed for unrealized gains/losses.
  - `finance.tax.calculation.events.v1` – tax amounts computed for transactions.
  - `finance.approval.workflow.events.v1` – workflow state changes (submitted, approved, rejected).
  - `finance.consolidation.events.v1` – consolidation run completed with elimination entries.
  - `finance.bank.reconciliation.events.v1` – bank statement reconciled with exceptions flagged.
  - `finance.compliance.alert.events.v1` – regulatory threshold breaches or control violations.

#### Inbound Events
- **Core Events (Phase 4)**
  - `identity.domain.events.v1` – to sync tenant/user metadata for audit logging.
  - `commerce.order.events.v1`, `procurement.payables.events.v1` – to auto-generate journal entries.

- **Enterprise Events (Phase 5)**
  - `treasury.payment.events.v1` – payment execution triggers cash reconciliation.
  - `inventory.valuation.events.v1` – inventory adjustments require cost of goods sold postings.
  - `payroll.run.events.v1` – salary payments create journal entries.
  - `tax.return.filed.events.v1` – tax filing confirmation for accrual reversal.
  - `intercompany.transaction.events.v1` – cross-entity transactions for elimination tracking.

## Context
- Phase 4 requires a demonstrable financial slice before onboarding other contexts.
- Financial data has the highest integrity/compliance requirements (SOX, audit, GDPR retention).
- Existing ADRs (002, 003, 005, 006) demand strong isolation, clear contracts, and governance; we must extend those to finance.
- **Business Scalability Requirements**: System must support SMBs (simple ledger) through enterprises (multi-entity consolidation, 100+ legal entities).
- **Regulatory Complexity**: Need to support multiple accounting standards (GAAP, IFRS), tax jurisdictions, and industry-specific requirements.
- **Performance Targets**: Journal posting <200ms p95, period close for 1M transactions <5 minutes, consolidation for 50 entities <15 minutes.
- **Integration Breadth**: Must integrate with 20+ upstream/downstream contexts plus external systems (banks, tax authorities, audit tools).

## Alternatives Considered

### Phase 4 Alternatives
1. **Embed financial logic into commerce / shared modules**  
   Rejected: Violates bounded-context autonomy and complicates compliance boundaries.
2. **Defer ledger modeling until later phases**  
   Rejected: Phase 4 success criteria explicitly require a vertical slice with measurable SLIs.
3. **Adopt event sourcing + bespoke storage from day one**  
   Deferred: Adds risk/complexity; we can layer event sourcing later on top of the CQRS baseline.

### Phase 5 Alternatives
4. **Use external financial system (SAP, Oracle) instead of building**  
   Rejected: High licensing costs ($50K-$500K/year), limited customization, vendor lock-in, integration complexity.
5. **Feature flags for all enterprise capabilities**  
   **Accepted**: Use configuration-based feature enablement to allow SMBs to start simple and grow into complexity.
6. **Separate microservices for each dimension (currency, tax, consolidation)**  
   Rejected: Over-fragmentation increases operational overhead; bounded context should own its complete domain.
7. **Blockchain for immutable audit trail**  
   Deferred: Traditional database audit logging with write-ahead log meets current compliance needs; revisit for specific industries (e.g., crypto).
8. **Real-time consolidation vs. batch processing**  
   Hybrid: Real-time for operational dashboards, batch for regulatory reporting and complex eliminations.

## Consequences

### Positive
- **Scalability**: Feature flags allow SMBs to start with basic ledger and scale to full enterprise capabilities.
- **Compliance Ready**: Built-in support for SOX, IFRS, GAAP reduces audit costs and regulatory risk.
- **Clear Boundaries**: ADR documentation with aggregate boundaries enables parallel team scaling.
- **Schema Isolation**: Tenant-level data isolation meets multi-tenancy security requirements.
- **Integration Ready**: Event-driven architecture enables seamless connection to 20+ contexts.
- **Cost Efficiency**: Avoids $50K-$500K/year SAP licensing costs while maintaining feature parity.
- **Customization**: Full control over domain logic enables industry-specific adaptations.
- **Performance**: Optimistic locking, bulk operations, and caching strategies meet enterprise SLAs.

### Negative
- **Complexity**: Full feature set requires 15-20 domain aggregates vs. 4-5 in basic implementation.
- **Build Time**: Comprehensive test suite (unit, integration, performance) increases CI time by 5-10 minutes.
- **Migration Effort**: Schema migrations for Phase 5 features require careful planning with zero-downtime strategies.
- **Learning Curve**: Team needs expertise in accounting, tax, treasury, and consolidation domains.
- **Maintenance**: Configuration matrix (SMB vs. enterprise features) requires clear documentation and testing.
- **Performance Tuning**: Consolidation for 100+ entities requires query optimization, partitioning, and caching.

### Neutral
- **Outbox Pattern**: Consistent with identity context; operational monitoring extended to financial events.
- **Tech Stack**: No new technologies introduced; continues Kotlin, Quarkus, PostgreSQL, Kafka patterns.
- **Deployment**: Standard Helm charts with environment-specific configurations (dev/staging/prod).

### Business Impact by Segment

#### SMB (1-50 employees, single entity)
- **Enabled Features**: Basic ledger, single currency, simple chart of accounts, manual journal entries
- **Time to Value**: 2-4 weeks implementation
- **User Complexity**: Low (5-10 screens)
- **Cost**: Minimal infrastructure ($100-500/month)

#### Mid-Market (50-500 employees, 2-10 entities)
- **Enabled Features**: Multi-currency, cost centers, basic approval workflows, intercompany transactions
- **Time to Value**: 2-3 months implementation
- **User Complexity**: Medium (20-30 screens)
- **Cost**: Moderate infrastructure ($500-2000/month)

#### Enterprise (500+ employees, 10-100+ entities)
- **Enabled Features**: Full suite including consolidation, complex workflows, regulatory reporting, bank reconciliation
- **Time to Value**: 6-12 months implementation
- **User Complexity**: High (50+ screens, extensive configuration)
- **Cost**: Enterprise infrastructure ($2000-10000/month)

## Compliance / Enforcement

### Development Standards
1. **Schema & Migrations**
   - Flyway scripts prefixed `V_FA_###` scoped to `financial_accounting` schema
   - Zero-downtime migrations with backward compatibility guaranteed for N-1 version
   - All currency columns use `BIGINT` (store as minor units, e.g., cents) to avoid floating-point errors
   - Audit columns: `created_by`, `created_at`, `modified_by`, `modified_at`, `version` on all tables

2. **Security & Authorization**
   - Gateway scopes: `financial-admin`, `financial-user`, `financial-auditor`, `financial-approver`
   - Row-level security: Users can only access ledgers/journals for their assigned entities
   - Sensitive operations (period close, approval override) require MFA
   - API rate limiting: 100 req/min for reads, 20 req/min for writes per user

3. **Observability & Monitoring**
   - Business metrics: `finance.journal.post.duration`, `finance.period.close.duration`, `finance.consolidation.duration`
   - Alert thresholds: p95 > 200ms (journal post), p95 > 5min (period close), error rate > 0.1%
   - Distributed tracing with correlation IDs across all financial operations
   - Audit log retention: 7 years (regulatory requirement)

4. **Quality Gates (CI/CD)**
   - Unit tests: 90% coverage minimum for domain and application layers
   - Integration tests: All repository operations, REST endpoints, event publishers
   - Contract tests: Verify event schemas against schema registry
   - Performance tests: Load test with 1000 journals/second, validate <200ms p95
   - Chaos tests: Database failover, Kafka outage, network partition scenarios

5. **Event Governance**
   - All events versioned following `{context}.{aggregate}.events.v{N}` convention
   - Schemas stored in `docs/schemas/finance/` and registered in Apicurio Schema Registry
   - Breaking changes require new version; non-breaking changes allowed within version
   - Event replay capability for audit and disaster recovery

### Regulatory Compliance

6. **SOX Compliance**
   - Segregation of duties: Separate roles for journal entry vs. approval
   - Change tracking: All modifications to posted journals create audit trail
   - Access controls: Principle of least privilege enforced at API level
   - Regular access reviews: Automated reports for compliance team

7. **GAAP/IFRS Support**
   - Configurable accounting policies per ledger (accrual vs. cash basis)
   - Support for both historical cost and fair value accounting
   - Revenue recognition rules aligned with ASC 606 / IFRS 15
   - Lease accounting support for ASC 842 / IFRS 16

8. **Tax Compliance**
   - Tax code validation against jurisdiction rules
   - Automatic tax calculation with override capability and justification
   - Tax reporting by jurisdiction with reconciliation to general ledger
   - Integration hooks for external tax engines (Avalara, Vertex, SAP Tax)

9. **Data Retention & Privacy**
   - Financial records retained for 7 years (configurable by jurisdiction)
   - GDPR right-to-erasure: Pseudonymization for personal data after retention period
   - Data residency: Multi-region support with data sovereignty compliance
   - Encryption: At-rest (AES-256) and in-transit (TLS 1.3)

## Feature Configuration Framework

### Configuration-Based Enablement
Features are enabled via tenant configuration to support different business sizes and complexities:

```yaml
financial_features:
  tier: "SMB" | "MIDMARKET" | "ENTERPRISE"
  
  core:
    basic_ledger: true              # Always enabled
    chart_of_accounts: true         # Always enabled
    journal_entries: true           # Always enabled
    accounting_periods: true        # Always enabled
  
  currency:
    multi_currency: false           # SMB: false, Mid+: true
    currency_translation: false     # SMB: false, Enterprise: true
    unrealized_gains: false         # SMB: false, Enterprise: true
  
  dimensions:
    cost_centers: false             # SMB: false, Mid+: true
    profit_centers: false           # SMB: false, Enterprise: true
    projects: false                 # SMB: false, Mid+: true
    departments: false              # SMB: false, Mid+: true
    business_units: false           # SMB: false, Enterprise: true
  
  workflows:
    approval_workflows: false       # SMB: false, Mid+: true
    multi_level_approval: false     # SMB: false, Enterprise: true
    delegation: false               # SMB: false, Enterprise: true
  
  compliance:
    tax_engine: "SIMPLE"            # SMB: SIMPLE, Mid: STANDARD, Enterprise: ADVANCED
    sox_controls: false             # SMB: false, Enterprise: true
    regulatory_reporting: false     # SMB: false, Enterprise: true
  
  advanced:
    intercompany: false             # SMB: false, Mid+: true
    consolidation: false            # SMB: false, Enterprise: true
    bank_reconciliation: false      # SMB: false, Mid+: true
    cash_flow_forecasting: false   # SMB: false, Enterprise: true
```

### Migration Paths
- **SMB → Mid-Market**: Enable multi-currency, cost centers, basic approvals; estimated migration: 1-2 weeks
- **Mid-Market → Enterprise**: Enable consolidation, complex workflows, regulatory reporting; estimated migration: 1-3 months
- **Downgrade**: Not supported due to data model dependencies; export-import required

## Related ADRs
- [ADR-001](ADR-001-modular-cqrs.md) – CQRS conventions applied to ledger commands/queries.
- [ADR-002](ADR-002-database-per-context.md) – Schema-per-context strategy.
- [ADR-003](ADR-003-event-driven-integration.md) – Outbox/Event architecture reused.
- [ADR-005](ADR-005-multi-tenancy-isolation.md) – Tenant isolation rules (RLS/filters).
- [ADR-006](ADR-006-platform-shared-governance.md) – Limits on shared-kernel leakage.
- [ADR-007](ADR-007-authn-authz-strategy.md) – Authorization for financial roles and scopes.
- [ADR-008](ADR-008-cicd-network-resilience.md) – CI/CD pipeline for financial module deployment.

## Appendices

### Appendix A: Multi-Currency Implementation Strategy
- Exchange rates stored with effective date, rate type (spot/average/closing), and source
- Journal lines support mixed currencies within entry; automatic conversion to ledger base currency
- Unrealized gains/losses calculated monthly via scheduled job
- Currency translation for consolidation uses closing rate for balance sheet, average rate for P&L

### Appendix B: Tax Engine Integration Points
- `TaxCalculationService` interface for pluggable tax engines
- Built-in simple tax engine for basic VAT/GST (single rate per jurisdiction)
- Integration adapters for Avalara, Vertex, SAP Tax via REST APIs
- Tax codes linked to GL accounts; validation ensures tax accounts exist

### Appendix C: Consolidation Algorithm
1. **Prepare**: Translate subsidiary financials to parent currency using configured rates
2. **Eliminate**: Remove intercompany transactions (payables/receivables, sales/purchases)
3. **Adjust**: Apply minority interest calculations for partial ownership
4. **Aggregate**: Sum translated and adjusted balances across all entities
5. **Report**: Generate consolidated financial statements with drill-down capability

### Appendix D: Performance Optimization Strategies
- **Partitioning**: Journal entries partitioned by accounting period for faster queries
- **Indexing**: Composite indexes on (tenant_id, ledger_id, period_id, account_id)
- **Caching**: Chart of accounts, exchange rates, tax codes cached for 5 minutes
- **Bulk Operations**: Batch API accepts 1000+ journal lines in single transaction
- **Read Replicas**: Query APIs use read replicas to offload reporting workload

### Appendix E: Integration Patterns
- **Upstream Systems**: AP/AR, Payroll, Inventory, Fixed Assets publish events that create journal entries
- **Downstream Systems**: BI, Reporting, External Auditors consume journal and period events
- **External Systems**: Banks (statement import), Tax Authorities (e-filing), Auditors (data export)
- **API Gateway**: Rate limiting, authentication, and routing for all financial endpoints

### Appendix F: Current Implementation Status (Phase 4)

**✅ Completed Features:**
- Core domain model: Ledger, ChartOfAccounts, Account, AccountingPeriod, JournalEntry with double-entry validation
- Value classes for type safety: LedgerId, AccountId, ChartOfAccountsId, AccountingPeriodId, Money (stored as BIGINT)
- Application layer: AccountingCommandHandler with CreateLedger, DefineAccount, PostJournalEntry, CloseAccountingPeriod
- Persistence: Flyway schema V001 with financial_accounting schema, JPA entities with optimistic locking (@Version)
- REST API: `/api/v1/finance/**` endpoints (ledgers, chart-of-accounts, journal-entries, periods)
- Event publishing: Outbox pattern with `finance.journal.events.v1` and `finance.period.events.v1`
- Testing: 25+ @Test methods across domain, application, and infrastructure layers (38 test files total)
- Infrastructure: Quarkus + PostgreSQL + Kafka + Micrometer + Flyway + Hibernate Validator

**🟡 Partially Complete:**
- API Gateway integration: REST endpoints implemented, gateway proxy/scopes pending
- Observability: Infrastructure present (Micrometer, Prometheus), metrics annotations pending
- Health checks: quarkus-smallrye-health dependency added, custom checks pending

**❌ Not Yet Implemented (Phase 5 Targets):**
- Query/Reporting APIs: Trial balance, general ledger reports, financial statements
- Multi-currency support: ExchangeRate aggregate, foreign currency journal lines
- Cost centers & dimensions: Multi-dimensional accounting framework
- Approval workflows: Maker-checker pattern, multi-level approvals
- AP/AR modules: Currently only placeholder objects (BillPlaceholder, InvoiceResourcePlaceholder, etc.)
- financial-shared module: Money, Currency, FiscalPeriod are placeholders
- Advanced features: Bank reconciliation, consolidation, tax engine, regulatory reporting

**Critical Gaps Identified:**
1. **Metrics Annotations Missing**: No @Counted or @Timed on command handlers or REST endpoints
2. **Health Checks Missing**: No custom readiness/liveness probes for financial-specific health
3. **AP/AR Placeholders**: Entire AP and AR modules are stubs (not production-ready)
4. **Shared Financial Types**: financial-shared module needs proper Money, Currency implementations
5. **API Documentation**: No .rest files for Postman/IntelliJ HTTP Client testing
6. **Load Testing**: No k6/JMeter scripts for performance validation
7. **Chaos Testing**: No chaos experiments defined

**Database Schema Notes:**
- Uses BIGINT for all monetary amounts (stores minor units: cents, pence, etc.) - prevents floating-point errors ✅
- Optimistic locking via version column on all mutable entities ✅
- Unique constraints on tenant + code combinations ✅
- Check constraint ensures journal lines have either debit or credit (not both) ✅
- Foreign key cascades on journal_entry_lines for referential integrity ✅
- Missing: Audit columns (created_by, modified_by) - only timestamps present ⚠️
- Missing: Foreign currency columns (foreign_currency, foreign_amount, exchange_rate) ⚠️
- Missing: Dimension columns (cost_center_id, project_id, department_id) ⚠️

**Recommendations for Immediate Action:**
1. Add @Timed annotations to all command handlers and REST endpoints
2. Create FinanceHealthCheck implementing HealthCheck interface
3. Generate .rest files for API testing (finance-api.rest)
4. Implement proper Money value object in financial-shared (replace placeholder)
5. Add created_by/modified_by audit columns to all tables
6. Document API Gateway scope requirements (financial-admin, financial-user, financial-auditor)
