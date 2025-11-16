# Validation Migration & Performance Guide

_Version: November 16, 2025_

This guide helps teams migrate legacy REST endpoints to the ADR‑010 validation pattern and tune performance.

## 1. Migration Checklist

| Step | Description | Files |
|------|-------------|-------|
| 1 | Identify endpoints still using manual parsing | `git grep "UUID.fromString("` |
| 2 | Create request DTOs annotated with `@Valid @BeanParam` | `bounded-contexts/*/*-infrastructure/.../dto` |
| 3 | Add constraint annotations (`financial-shared/validation/validators`) | e.g., `@ValidAccountCode`, `@ValidCurrencyCode` |
| 4 | Implement `toCommand()/toQuery()` converting DTO → domain command | Command resource DTO |
| 5 | Throw `FinanceValidationException` w/ error code + localized message | Command resource DTO |
| 6 | Wire DTO into resource method replacing manual parsing | `*-infrastructure/.../rest/*Resource.kt` |
| 7 | Update tests to expect HTTP 422 + structured error payload | `*-infrastructure/.../test/...` |
| 8 | Document new error codes in `ValidationMessages*.properties` | Shared resources |

## 2. ValidationProblemDetail Schema

All REST services must expose the shared schema in OpenAPI:

```yaml
ValidationProblemDetail:
  type: object
  required: [code, message]
  properties:
    code: { type: string, example: FINANCE_INVALID_VENDOR_ID }
    message: { type: string }
    validationErrors:
      type: array
      items:
        type: object
        properties:
          field: { type: string }
          code: { type: string }
          message: { type: string }
          rejectedValue: { type: string }
```

Reference: `bounded-contexts/financial-management/financial-accounting/accounting-infrastructure/src/main/resources/META-INF/openapi/error-responses.yaml`

## 3. Performance Tuning

| Lever | Knob | Default | Notes |
|-------|------|---------|-------|
| Vendor cache | `validation.performance.cache.vendor.*` | max-size=10000, ttl=PT5M | Adjust per tenant volume |
| Customer cache | `validation.performance.cache.customer.*` | max-size=10000, ttl=PT5M | Applies to AR service |
| Rate limiting | `validation.security.rate-limit.*` | default=100/min | Lower for SOX-critical paths |
| Circuit breaker | `ValidationCircuitBreaker` | failureRatio=0.5, delay=5s | Override via CDI config if needed |

### Tips
- Monitor `validation.cache.hitratio` after changes; target > 0.8
- If caches thrash, increase TTL or reduce eviction frequency
- Use `histogram_quantile` dashboards to verify latency improvements

## 4. Example Migration (Vendor Resource)

1. Added `VendorRequest` DTO with constraint annotations.
2. Resource signature: `fun registerVendor(@Valid request: VendorRequest): Response`.
3. `VendorCommandService` now uses `VendorExistenceCache`.
4. Tests updated in `VendorResourceValidationTest.kt`.

## 5. Documentation Links

- ADR-010 Standard
- `docs/guides/VALIDATION_DEVELOPER_GUIDE.md`
- `docs/VALIDATION_ARCHITECTURE.md`
- `docs/runbooks/VALIDATION_OPERATIONS.md`
