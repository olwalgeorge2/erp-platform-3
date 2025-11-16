# Validation Architecture Overview

_Version: November 2025_

This document captures the cross-cutting REST validation layer described in ADR‑010 and implemented across the ERP bounded contexts.

---

## 1. Component View

| Layer | Components | Location |
|-------|------------|----------|
| **Ingress Filters** | `ValidationMetricsFilter`, `ValidationRateLimitFilter`, `InputSizeLimitFilter`, `ValidationAuditFilter` | `financial-shared/src/main/kotlin/com/erp/financial/shared/rest` |
| **DTO/Constraint Layer** | DTOs annotated with Bean Validation constraints + custom validators (`ValidAccountCode`, `ValidAmount`, `ValidDateRange`, etc.) | `bounded-contexts/*/*-infrastructure/.../dto` + `financial-shared/validation/validators` |
| **Security/Performance Utilities** | `ValidationRateLimiter`, `ValidationCircuitBreaker`, existence caches (`VendorExistenceCache`, `CustomerExistenceCache`) | `financial-shared/validation/security`, `ap|ar-application/cache` |
| **Exception Handling** | `FinanceValidationExceptionMapper`, `FinanceValidationErrorCode`, localized message bundles | `financial-shared/validation`, `financial-shared/rest`, `financial-shared/src/main/resources` |
| **Observability** | `ValidationMetrics`, Grafana dashboards (`validation-performance`, `validation-quality`), Prometheus alerts (`validation-alerts.yml`) | `financial-shared/validation/metrics`, `dashboards/grafana`, `monitoring/prometheus` |

---

## 2. Sequence Diagrams (Textual)

### 2.1 Happy Path (Create Vendor)
1. HTTP POST `/api/v1/finance/vendors`
2. `ValidationMetricsFilter` timestamps request
3. `ValidationRateLimitFilter` enforces bucket → allowed
4. DTO (`VendorRequest`) performs Bean Validation (custom validators fire)
5. `VendorCommandService` resolves vendor existence via `VendorExistenceCache` (cache miss → repository → cache populate)
6. Domain logic executes, repository persists entity
7. `FinanceValidationExceptionMapper` not invoked; response returns `201 Created`

### 2.2 Rate Limit Violation
1. High-frequency client exceeds SOX bucket
2. `ValidationRateLimitFilter` aborts with `FINANCE_RATE_LIMIT_EXCEEDED`
3. `ValidationMetrics` records `validation.rate_limit.violations`
4. Audit log entry emitted (`duration_ms`, bucket info)
5. Prometheus alert `FinanceRateLimitAbuse` fires if sustained

### 2.3 Repository Failure
1. Vendor lookup repeatedly times out
2. `ValidationCircuitBreaker` for `vendor_lookup` opens
3. Circuit breaker raises `FINANCE_DEPENDENCY_UNAVAILABLE`
4. Exception mapper returns localized 503 response (no internals leaked)
5. Audit log + metrics capture the dependency outage

---

## 3. Caching Flow

```
Service -> VendorExistenceCache -> (hit?) -> return vendor
                                -> (miss) -> CircuitBreaker.guard { repository.findById() }
                                -> cache.put(result) / propagate exception
```

Cache tuning knobs:
```
validation.performance.cache.vendor.max-size=10000
validation.performance.cache.vendor.ttl=PT5M
validation.performance.cache.customer.max-size=10000
validation.performance.cache.customer.ttl=PT5M
```

Metrics:
- `validation.cache.size{cache="vendor-existence"}`
- `validation.cache.hitratio{cache="vendor-existence"}`
- `validation.cache.missratio{cache="vendor-existence"}`

---

## 4. Documentation Links

- ADR-010 Standard: `docs/adr/ADR-010-rest-validation-standard.md`
- Developer Guide: `docs/guides/VALIDATION_DEVELOPER_GUIDE.md`
- Runbook: `docs/runbooks/VALIDATION_OPERATIONS.md`
- Pattern Reference: `docs/REST_VALIDATION_PATTERN.md`
- Implementation Summary: `docs/REST_VALIDATION_IMPLEMENTATION_SUMMARY.md`

---

## 5. Future Enhancements

- Extend caches to accounting ledger/account validators
- Add OpenTelemetry spans around validator execution
- Automate cache warming during deployment rollouts
