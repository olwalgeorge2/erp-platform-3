# Validation Operations Runbook

_Last updated: November 16, 2025_

This runbook covers the most common incidents and operational procedures for the ADR‑010 validation layer.

---

## 1. Monitoring Sources

| Source | Location | Notes |
|--------|----------|-------|
| Grafana Dashboards | `validation-performance`, `validation-quality` | Latency, throughput, failure analytics |
| Prometheus Alerts | `monitoring/prometheus/validation-alerts.yml` | Latency (>100 ms p95), failure rate (>5 %), SOX/PII breaches, rate-limit abuse |
| Logs | Loki query `{app="finance"} |= "validation failure"` | Structured JSON logs with tenant, user, duration |
| Metrics API | `/q/metrics` | `validation.request.duration`, `validation.rule.duration`, `validation.rate_limit.violations`, `validation.cache.*` |

---

## 2. Common Incidents

### 2.1 High Validation Failure Rate (> 5%)
**Alert**: `FinanceValidationFailureRateHigh`

**Runbook**:
1. Open Grafana → `validation-quality` → identify top failing error codes/fields.
2. Inspect audit logs for affected tenants/users.
3. If tenant-specific, engage tenant support with error details.
4. If systemic, coordinate with feature team to rollback offending change.

### 2.2 Latency Spike (p95 > 100 ms)
**Alert**: `FinanceValidationLatencyHigh`

**Runbook**:
1. Grafana `validation-performance` panel → confirm spike.
2. Check `validation.rule.duration` for slow validators.
3. Review caches (`validation.cache.hitratio`) to ensure > 0.8. If low, inspect repository health.
4. Escalate to platform team if downstream dependency degraded.

### 2.3 Rate Limit Violations
**Alert**: `FinanceRateLimitAbuse`

**Runbook**:
1. Query logs for `bucket="validation-sox"` to locate offending IPs/users.
2. Coordinate with security team if malicious.
3. Adjust `validation.security.rate-limit.*` knobs only after assessing business impact.

### 2.4 Circuit Breaker Open (Dependency Unavailable)
**Symptom**: Many `FINANCE_DEPENDENCY_UNAVAILABLE` responses.

**Runbook**:
1. Check service logs for circuit breaker messages.
2. Validate downstream repository health (DB, network).
3. Once dependency recovers, monitor breaker metrics to ensure it closes (success threshold = 3).

### 2.5 Cache Thrashing
**Indicators**: `validation.cache.hitratio` < 0.4, `validation.cache.size` oscillating.

**Runbook**:
1. Verify deployment frequency—cache TTL may be too short.
2. Increase `validation.performance.cache.*.ttl` temporarily if needed.
3. Ensure evictions only occur on actual mutations.

---

## 3. Operational Knobs

| Setting | Location | Purpose |
|---------|----------|---------|
| `validation.security.rate-limit.*` | Finance service `application.yml` | Adjust rate-limit buckets |
| `validation.security.abuse.*` | Finance service `application.yml` | Set abuse detection thresholds |
| `validation.performance.cache.*` | Finance service `application.yml` | Tune cache TTL and size |
| `validation.metrics.context` | Finance service `application.yml` | Metric tag used by dashboards |

Changes to these settings require change-management approval and redeploying the affected service.

---

## 4. Escalation Matrix

| Severity | Condition | Contacts |
|----------|-----------|----------|
| Sev1 | Platform-wide validation failure rate >10% or dependency outage | Platform SRE on-call + Finance Platform Tech Lead |
| Sev2 | Single tenant failure > 5% for >30 min | Tenant Success + Finance Platform |
| Sev3 | Rate limit abuse from known IP range | Security Operations |

Escalate via Slack `#incident-response` and PagerDuty according to the severity matrix.

---

## 5. Maintenance Checklist

- [ ] Review dashboards weekly for slow validator trends
- [ ] Validate alert thresholds quarterly
- [ ] Audit error code documentation each release (align with ADR-010)
- [ ] Ensure developer guide stays in sync with implementation changes
