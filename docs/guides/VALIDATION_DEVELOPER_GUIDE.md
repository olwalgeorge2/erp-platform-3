# Validation Developer Guide

_Last updated: November 16, 2025_

This guide explains how to extend the ADR‑010 validation framework that powers the ERP platform. It assumes familiarity with Kotlin, Quarkus, and Jakarta Bean Validation.

---

## 1. Architecture Overview

```
HTTP Request
   │
   ├─ ValidationMetricsFilter (timers/counters)
   ├─ ValidationRateLimitFilter (security buckets)
   ├─ DTO (@Valid @BeanParam) → toCommand()/toQuery()
   │     └─ Custom ConstraintValidators (financial-shared/validation/validators)
   ├─ Application Service (*/application/service/*)
   │     └─ ValidationCircuitBreaker + caches (e.g., VendorExistenceCache)
   ├─ Domain / Repository
   └─ FinanceValidationExceptionMapper (422 envelope + audit logging)
```

Key shared artifacts live in `bounded-contexts/financial-management/financial-shared`:

- `validation/validators/*`: reusable constraint validators
- `validation/security/*`: rate limiting + circuit breaker utilities
- `validation/metrics/*`: Micrometer plumbing
- `rest/ValidationMetricsFilter.kt`, `rest/ValidationAuditFilter.kt`

---

## 2. Creating a New Custom Validator

1. **Define the Constraint Annotation**
   ```kotlin
   @Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
   @Retention(AnnotationRetention.RUNTIME)
   @Constraint(validatedBy = [MyValidator::class])
   annotation class ValidMyThing(
       val message: String = "{FINANCE_INVALID_MY_THING}",
       val groups: Array<KClass<*>> = [],
       val payload: Array<KClass<out Payload>> = [],
   )
   ```

2. **Implement `ConstraintValidator`**
   - Place the class in `financial-shared/src/main/kotlin/.../validation/validators`
   - Inject collaborators via CDI if needed
   - Record metrics via `ValidationMetrics.recordRule(ruleName, durationNanos, success)`

   ```kotlin
   @ApplicationScoped
   class MyValidator(
       private val dependency: SomeService,
   ) : ConstraintValidator<ValidMyThing, String?> {
       override fun isValid(value: String?, ctx: ConstraintValidatorContext?): Boolean {
           val start = System.nanoTime()
           val result =
               when {
                   value == null -> true
                   value.isBlank() -> false
                   else -> dependency.isAllowed(value)
               }
           ValidationMetrics.recordRule("my_validator", System.nanoTime() - start, result)
           return result
       }
   }
   ```

3. **Reference the Constraint in DTOs**
   ```kotlin
   data class CreateFooRequest(
       @field:ValidMyThing
       val payload: String,
   )
   ```

4. **Add Localized Messages**
   - Append keys to `ValidationMessages*.properties`
   - Use the format `FINANCE_INVALID_*`

5. **Write Unit Tests**
   - Place tests under `financial-shared/src/test/.../validators`
   - Use `HibernateValidator` test harness + MockK where applicable

---

## 3. Error Code Allocation

| Range | Usage |
|-------|-------|
| `FINANCE_INVALID_*` | Field-level validation failures (HTTP 400/422) |
| `FINANCE_RATE_LIMIT_EXCEEDED` | Security bucket violations (HTTP 429) |
| `FINANCE_DEPENDENCY_UNAVAILABLE` | Circuit breaker fallbacks (HTTP 503) |

Guidelines:
- Reuse existing codes when semantics match
- Add new entries to `FinanceValidationErrorCode.kt` + all `ValidationMessages*.properties`
- Keep names functional (e.g., `FINANCE_INVALID_VENDOR_NUMBER`)

---

## 4. Testing Strategies

- **Unit tests** for each constraint validator (happy path + failure cases)
- **DTO tests** verifying `toCommand()` conversions and error mapping
- **Integration tests** (Quarkus with mocks) to assert HTTP 422 payloads
- **Property-based tests** for complex cross-field validations (see `VALIDATION_PHASE3_PLAN.md`)
- **Performance smoke tests** when adding caching or circuit breakers

---

## 5. Performance Best Practices

- Prefer fast in-memory checks before expensive repository calls
- Share caches (e.g., `VendorExistenceCache`) for high-frequency lookups
- Wrap repository access with `ValidationCircuitBreaker` + `@Timeout`
- Emit metrics via `ValidationMetrics` to spot regressions

---

## 6. Troubleshooting & Operations

- **Metric Sources**
  - `/q/metrics` → `validation.request.duration`, `validation.rule.duration`, `validation.rate_limit.violations`
  - Grafana dashboards: `validation-performance`, `validation-quality`
- **Audit Logs** contain `duration_ms`, tenant, user, and error code
- **Runbook**: see `docs/runbooks/VALIDATION_OPERATIONS.md`
- **Rate Limits** configurable via `validation.security.rate-limit.*`
- **Caches** tunable via `validation.performance.cache.*`

---

## 7. References

- ADR-010: `docs/adr/ADR-010-rest-validation-standard.md`
- Validation Pattern: `docs/REST_VALIDATION_PATTERN.md`
- Implementation Summary: `docs/REST_VALIDATION_IMPLEMENTATION_SUMMARY.md`
- Architecture Views: `docs/VALIDATION_ARCHITECTURE.md`
- Runbook: `docs/runbooks/VALIDATION_OPERATIONS.md`

Need help? Ping `#team-finance-platform` on Slack.
