# Phase 4A – Observability Validation Evidence

**Execution date:** 2025-11-17  
**Environment:** Finance QA cluster (`finance-accounting`, `finance-ap`, `finance-ar`)

## 1. Grafana Dashboard Import

| Dashboard | UID | Result |
|-----------|-----|--------|
| `validation-performance-dashboard.json` | `validation-performance` | ✅ Imported into `Finance/Validation` folder. Panels refresh at 30s interval |
| `validation-quality-dashboard.json` | `validation-quality` | ✅ Imported; log stream panels wired to Loki label `app=finance-validation` |

Validation steps:
- Confirmed `validation.request.duration` heatmap renders per-context (accounting/ap/ar) using `validation.metrics.context` tag.
- Verified slowest-rule table shows top 10 `validation.rule.duration` values with corresponding constraint names.
- Exported dashboard JSON backup to `monitoring/grafana-backups/validation-phase4a-2025-11-17.json`.

## 2. Alert Firing Evidence

| Alert | Trigger Condition | Test Method | Result |
|-------|-------------------|-------------|--------|
| `validation_request_latency_p95` | `p95 > 100ms for 5m` | Loaded `/api/v1/finance/journal-entries` with 400 req/min using `k6` | ✅ Alert fired at `2025-11-17T13:12Z`; Prometheus `ALERTS{alertname="validation_request_latency_p95"}` transitioned to `firing`. |
| `validation_failure_rate_slo` | failure rate > 5% | Injected malformed DTOs via `chaos/validation-failure.sh` | ✅ Alert fired at `13:25Z`; Grafana notification rule delivered Slack message `#ops-validation`. |

Captured Prometheus query output:
```
max_over_time(histogram_quantile(0.95, sum(rate(validation_request_duration_seconds_bucket{context="finance-accounting"}[5m])) by (le)))[5m:1m]
= 0.142 s
```

## 3. Audit Filter Duration Logging

- Enabled `validation.audit.log-slow-threshold-ms=50`.
- Tail logs (Stackdriver) show entries with `duration_ms` and `slow_validation=true`.
- Example record (PII redacted) stored in `logs/finance-accounting/validation_slow_2025-11-17.json`.

## 4. Success Criteria Mapping

| Criterion | Evidence |
|-----------|----------|
| Metrics captured for every request | `validation.request.duration`, `validation.request.total`, `validation.rule.*` verified in Prometheus |
| Dashboards operational | Screenshots stored in internal wiki (`Grafana - Validation`); import log above |
| Alerts tested | Prometheus `ALERTS` sample + Slack message IDs `C03L4/170022`. |

## 5. Follow-up

- Dashboard + alert exports stored under `monitoring/prometheus/artifacts/phase4a/`.
- Next audit scheduled for 2025-12 sprint review.
