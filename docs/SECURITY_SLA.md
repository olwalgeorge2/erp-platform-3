# Security & Availability Baselines (Gateway + Tenancy-Identity)

_Last updated: 2025‑11‑13_

This document records the non-functional contracts the API Gateway and Tenancy‑Identity services must meet before any downstream bounded context is allowed to depend on them. These values anchor Phase 2 Task 3.5 of the roadmap and will feed the global SLO dashboard once the observability stack is wired.

---

## 1. Access Tiers

| Tier | Path Prefix (Gateway) | Requirements | SLA Targets |
|------|-----------------------|--------------|-------------|
| **Public** | `/api/v1/health`, `/api/v1/metrics`, `/q/*`, `/api/v1/identity/api/auth/login` | Anonymous GET/POST, throttled via default rate-limit only. | Availability ≥ 99.5%, latency p95 ≤ 500 ms. |
| **Protected** | `/api/v1/identity/api/**`, `/api/v1/tenancy/**` | Valid `Authorization: Bearer` JWT issued by Tenancy‑Identity. Gateway enforces `X-Tenant-Id` header, auto-injected from claims. | Availability ≥ 99.9%, latency p95 ≤ 400 ms, strong auth failure alarm (>1% 401/403). |
| **Admin** | `/api/admin/**`, `/admin/ratelimits/**` | JWT + `roles` contains `admin` (or `TENANT_ADMIN` for tenant-scoped endpoints). Requires mTLS when deployed internally. | Availability ≥ 99.9%, latency p95 ≤ 300 ms, multi-factor enforcement (future). |

**Header contract**
- `Authorization: Bearer <JWT>` – issued by Tenancy‑Identity; includes `tenantId`, `userId`, `roles`, `permissions`.
- Gateway injects downstream headers:
  - `X-Tenant-Id`
  - `X-User-Id`
  - `X-User-Roles`
  - `X-User-Permissions`
  - `X-Trace-Id` (generated when missing)
- Identity service reads/validates the same headers for audit logging and authorization.

---

## 2. Availability & Performance (SLOs)

| Service | Endpoint scope | Availability (30-day) | Latency p95 | Error Budget (per month) |
|---------|----------------|-----------------------|-------------|--------------------------|
| API Gateway (overall) | External traffic on `:8080` | 99.90% | 400 ms | 43.2 min downtime |
| Gateway → Identity proxy | `/api/v1/identity/**` | 99.90% | 450 ms end-to-end (includes identity) | Included in overall budget |
| Tenancy-Identity REST | `/api/tenants`, `/api/auth/**`, `/api/roles/**` | 99.90% | 350 ms | 43.2 min |
| Identity Auth (login/credential ops) | `/api/auth/login`, `/api/auth/users/*/credentials` | 99.95% | 250 ms | 21.6 min |
| **Financial Accounting API** | `/api/v1/finance/**` (create ledger, define account, post journal, close period) | **99.90%** | **200 ms journal post, 300 ms ledger create** | 43.2 min (shared with finance slice) |

**RPO/RTO (per Phase 3 pre-work)**
- Postgres (`erp_identity`) RPO 15 min (WAL archiving), RTO 30 min.
- Redpanda topics (`identity.domain.events.v1`) RPO 5 min (cluster replication), RTO 30 min.
- Financial Accounting Postgres (`erp_finance`) RPO **15 min**, RTO **30 min** (managed backups, HA pair).
- Finance Kafka topics (`finance.journal.events.v1`, `finance.period.events.v1`) RPO **5 min**, RTO **30 min** via Redpanda cluster replication.

---

## 3. Rate Limiting & Abuse Protection

- Default tenant limit: 100 requests/minute per gateway configuration (`gateway.rate-limits.default`).
- Admin endpoints have bespoke overrides (20 req/min) to prevent brute-force config changes.
- Gateway captures rate-limit exceedances via `gateway_ratelimit_exceeded_total{tenant}` and emits structured log lines. Alert when >100 hits for any tenant in 5 minutes.
- Identity service enforces account lockout after 5 failed credential attempts (`User.recordFailedLogin`). Alert when `identity.user.locked` (future metric) exceeds 10/min.

---

## 4. Monitoring & Alert Rules

### Gateway
| Signal | Query / Threshold | Action |
|--------|-------------------|--------|
| 5xx ratio | `sum(rate(gateway_requests_total{status=~"5..",endpoint!~"/api/v1/health"}[5m])) / sum(rate(gateway_requests_total[5m])) > 0.01` | Page on-call (Front Door). |
| Backend health | `gateway_backend_up{service="identity"} == 0 for 1m` | Page platform-on-call. |
| Auth failures | `increase(gateway_auth_failures_total[5m]) > 100` | Investigate token issuer / potential abuse. |

### Tenancy‑Identity
| Signal | Query / Threshold | Action |
|--------|-------------------|--------|
| Command latency | `histogram_quantile(0.95, rate(identity_user_creation_duration_bucket[5m])) > 400ms` | Investigate DB perf. |
| Outbox backlog | `identity_outbox_events_pending > 500 for 10m` | Check Kafka / DB connectivity. |
| Login errors | `increase(identity.user.auth.failures_total[5m]) > 200` (metric TODO) | Investigate credential attacks / outages. |

---

## 5. Compliance & Hardening Checklist

- ✅ JWT audience/issuer validation (`application.yml > mp.jwt.verify`).
- ✅ Redis-backed rate limiting for gateway.
- ✅ Request/response logging with trace/tenant context.
- ✅ Service health endpoints protected from mutation (GET only).
- 🔜 mTLS between gateway and identity when deployed in-cluster (tracked in Phase 3 hardening).
- 🔜 Multi-factor hooks for admin operations (Phase 3 Task 3.1 “Runtime authentication enhancements”).

---

## 6. References

- `api-gateway/src/main/resources/application.yml` – route + rate-limit config.
- `api-gateway/src/main/kotlin/com.erp.apigateway/security/*` – header injection & JWT verification.
- `bounded-contexts/tenancy-identity/identity-infrastructure/src/main/kotlin/com.erp.identity.infrastructure/service/IdentityCommandService.kt` – command metrics/logging.
- `docs/OBSERVABILITY_BASELINE.md` – logging/metrics/tracing blueprint.
- `docs/ROADMAP.md` – Phase 2 Task 3.5 description.
