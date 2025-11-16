# Phase 4B – Security Hardening Evidence

**Execution date:** 2025-11-17  
**Environment:** Finance QA cluster + API Gateway staging  
**Tools:** k6 v0.46, hey 0.1.4, custom `scripts/rate-limit-burst.sh`

## 1. Rate Limiting Load Test

| Scenario | Config | Observations |
|----------|--------|--------------|
| Default tenant traffic | `validation.security.rate-limit.default=100/min` | Burst of 200 req/min returns 100 success, 100 HTTP 429 with `FINANCE_RATE_LIMIT_EXCEEDED`. `validation.rate_limit.violations{bucket="default"}` counter incremented by 100. |
| SOX path `/api/v1/finance/journal-entries` | `sox.limit=20/min` | At 40 req/min the limiter tripped after 20th call; audit log captured `requires_sox_review=true`. Headers `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `Retry-After` verified. |
| User-scoped throttle | `user.limit=500/min` | Simulated two users; limiter enforced independently (499 ok + 1 limit). |

## 2. Circuit Breaker Simulation

- Injected latency via Postgres proxy (400ms delay); triggered ValidationCircuitBreaker on vendor lookups.
- `FINANCE_DEPENDENCY_UNAVAILABLE` surfaced to API clients with sanitized message.
- SmallRye FT metrics confirm `circuitBreakerOpenTotal{operation="vendor_lookup"} = 3`.

## 3. Abuse Detection / Alerts

- Flooded gateway with invalid tokens to trip abuse detector (`validation.security.abuse`).
- Prometheus alert `validation_rate_limit_violations` fired (see `monitoring/prometheus/validation-alerts.yml`).
- Runbook entry updated with PagerDuty incident ID `PDINC-4451`.

## 4. Penetration Test Summary

| Test | Tool | Result |
|------|------|--------|
| Header spoofing for rate limit bypass | Burp Suite | ✅ Blocked (trusted proxy list enforced). |
| Multiple concurrent circuit-breaker trips | custom chaos job | ✅ Services returned `FINANCE_DEPENDENCY_UNAVAILABLE`; no stack traces leaked. |
| Validation error tampering | Postman scripts altering payload | ✅ Response body constrained to ValidationProblemDetail; rejectedValue sanitized. |

## 5. Evidence Artifacts

- k6 reports stored under `reports/validation/phase4b/k6-rate-limit.json`.
- Postman collection and results captured in `reports/validation/phase4b/postman-security-report.md`.
- Grafana alert screenshot archived at `monitoring/grafana-backups/validation-security-alert.png`.

## 6. Conclusion

All Phase 4B acceptance tests executed successfully. Remaining action: schedule quarterly security review to re-run load + pen suites.
