# Phase 4 Readiness Checklist – First Vertical Slice

_Last updated: 2025‑11‑13_

## 1. Candidate Context
- **Selected bounded context:** `financial-management/financial-accounting`
- **Why first:** Generates immediate business value (ledger, journal posting), has clear upstream dependencies (identity, gateway, platform-shared libraries), and exercises both synchronous APIs + event ingestion.

## 2. Entry Criteria
| # | Criterion | Status |
|---|-----------|--------|
| 1 | API Gateway + Tenancy-Identity in place with published contracts (Phase 2 complete) | ✅ |
| 2 | Data/messaging backbone documented w/ retention + recovery plans (Phase 3 Task 4.4) | ✅ |
| 3 | Schema isolation strategy ratified (ADR-002 addendum) | ✅ |
| 4 | Event versioning policy + registry workflow defined | ✅ |
| 5 | Security SLAs + RPO/RTO targets agreed (docs/SECURITY_SLA.md) | ✅ |
| 6 | Development environment supports new context scaffolding (Gradle settings, shared plugins) | ✅ |
| 7 | Operational runbooks exist for gateway & identity (observability baseline) | ✅ |
| 8 | Portal spike feedback captured (Phase 2 Task 3.6) | ❌ (defer to portal team) |

## 3. Deliverables for the Slice
1. **Domain modeling** – chart aggregates (`Ledger`, `JournalEntry`, `Account`) and publish to `bounded-contexts/financial-management/README`.
2. **Application layer** – CQRS command/query services, leveraging shared conventions.
3. **Persistence adapters** – schema `financial_accounting`, Flyway migrations `V_FA_xxx`.
4. **API exposure** – route via gateway under `/api/v1/finance/*` with corresponding auth scopes.
5. **Event integration** – consume `identity.domain.events.v1` for audit trails; emit `finance.journal.events.v1` (registered via event policy).
6. **Testing** – unit + Quarkus integration + contract tests; reuse Testcontainers pattern.
7. **SLIs** – measure p95 latency & success rate before expanding scope (ties back to Security SLA doc).

## 4. Risks & Mitigations
- **Dependence on accounting SMEs:** schedule domain discovery workshops up front (docs/ROADMAP §5.1).
- **Shared schema growth:** enforce schema-per-context migrations from day one.
- **Event fan-out:** ensure versioning policy is followed before publishing `finance.*` topics.

## 5. Next Steps
1. Kick off modeling session (Architecture + Finance SMEs) → update `CONTEXT_MAP.md`.
2. Scaffold Gradle modules under `bounded-contexts/financial-management/financial-accounting`.
3. Add gateway route + auth scopes to `api-gateway/src/main/resources/application.yml`.
4. Track progress in ROADMAP Phase 4 table once slice backlog is sized.
