# REST Validation Monitoring Guide

## Overview
This document provides queries and dashboards for monitoring REST validation failures across the ERP platform's Finance, Identity, and Gateway bounded contexts.

## Grafana Dashboard

**Location**: `dashboards/grafana/validation-monitoring-dashboard.json`

**Import Instructions**:
1. Open Grafana UI
2. Navigate to Dashboards → Import
3. Upload `validation-monitoring-dashboard.json`
4. Select Prometheus datasource
5. Select Loki datasource for log panels

**Dashboard Features**:
- Real-time validation error rates by context
- SOX-relevant failure alerts (Finance)
- Authentication/authorization failure tracking (Identity)
- Rate limit violation monitoring (Gateway)
- PII detection in validation failures (GDPR compliance)
- Top error codes and failed fields

## Prometheus Queries

### Validation Error Rates

```promql
# Overall error rate by context
rate(finance_validation_errors_total[5m])
rate(identity_validation_errors_total[5m])
rate(gateway_validation_errors_total[5m])

# Errors per minute
sum(rate(finance_validation_errors_total[5m])) * 60

# Top error codes
topk(10, sum by (error_code) (rate(finance_validation_errors_total[5m])))

# Top failed fields
topk(10, sum by (field) (rate(finance_validation_errors_total[5m])))
```

### Error Trends

```promql
# Hourly error counts
sum by (error_code) (increase(finance_validation_errors_total[1h]))

# Daily error totals
sum(increase(finance_validation_errors_total[24h]))

# Error rate comparison (current vs previous hour)
rate(finance_validation_errors_total[1h])
/
rate(finance_validation_errors_total[1h] offset 1h)
```

### Bounded Context Analysis

```promql
# Finance errors by bounded context label
sum by (bounded_context) (rate(finance_validation_errors_total[5m]))

# Specific error code tracking
finance_validation_errors_total{error_code="INVALID_AMOUNT"}
identity_validation_errors_total{error_code="INVALID_EMAIL"}
gateway_validation_errors_total{error_code="RATE_LIMIT"}
```

## Loki Queries (Log Aggregation)

### Finance Context

```logql
# All finance validation failures
{app="finance"} |= "validation_failure" | json

# SOX-relevant failures only
{app="finance"} |= "validation_failure" | json | requires_sox_review="true"

# Critical severity failures
{app="finance"} |= "validation_failure" | json | severity="CRITICAL"

# Failures with PII
{app="finance"} |= "validation_failure" | json | contains_pii="true"

# Specific tenant failures
{app="finance"} |= "validation_failure" | json | tenant_id="YOUR-TENANT-ID"

# Specific user failures
{app="finance"} |= "validation_failure" | json | user_id="YOUR-USER-ID"
```

### Identity Context

```logql
# Authentication failures
{app="identity"} |= "identity_validation_failure" | json | is_authentication_failure="true"

# Authorization failures
{app="identity"} |= "identity_validation_failure" | json | is_authorization_failure="true"

# Security review required
{app="identity"} |= "identity_validation_failure" | json | requires_security_review="true"

# Specific error codes
{app="identity"} |= "identity_validation_failure" | json | error_code="INVALID_CREDENTIALS"
```

### Gateway Context

```logql
# Rate limit violations
{app="gateway"} |= "gateway_validation_failure" | json | is_rate_limit_violation="true"

# Routing failures
{app="gateway"} |= "gateway_validation_failure" | json | is_routing_failure="true"

# DDoS pattern detection
{app="gateway"} |= "gateway_validation_failure" | json | severity="CRITICAL"

# Specific client IP
{app="gateway"} |= "gateway_validation_failure" | json | client_ip="192.168.1.100"
```

### Cross-Context Queries

```logql
# All validation failures across contexts
{app=~"finance|identity|gateway"} |= "validation_failure" | json

# High/Critical severity only
{app=~"finance|identity|gateway"} |= "validation_failure" | json | severity=~"HIGH|CRITICAL"

# Correlation ID tracking
{app=~"finance|identity|gateway"} |= "validation_failure" | json | correlation_id="YOUR-CORRELATION-ID"

# Time range with field extraction
{app="finance"} |= "validation_failure" | json | __timestamp__ >= 1700000000
```

## Splunk Queries

### Search Commands

```spl
# Finance validation failures
index=erp_platform app=finance "validation_failure" 
| spath 
| table _time, tenant_id, user_id, error_code, field, severity, requires_sox_review

# SOX compliance report
index=erp_platform app=finance "validation_failure" requires_sox_review=true
| stats count by error_code, field
| sort -count

# Top error codes by tenant
index=erp_platform app=finance "validation_failure"
| stats count by tenant_id, error_code
| sort tenant_id, -count

# Authentication failure analysis
index=erp_platform app=identity "identity_validation_failure" is_authentication_failure=true
| stats count by client_ip, user_agent
| where count > 10
| sort -count

# Rate limit violations
index=erp_platform app=gateway "gateway_validation_failure" is_rate_limit_violation=true
| timechart span=5m count by client_ip

# PII detection audit
index=erp_platform "validation_failure" contains_pii=true
| table _time, app, tenant_id, field, error_code
| sort -_time
```

### Dashboard Panels

```spl
# Validation errors by severity (pie chart)
index=erp_platform "validation_failure"
| stats count by severity

# Error trend over time (line chart)
index=erp_platform "validation_failure"
| timechart span=1h count by app

# Top 10 failed fields (bar chart)
index=erp_platform "validation_failure"
| stats count by field
| sort -count
| head 10
```

## Elasticsearch/Kibana Queries

### Query DSL

```json
{
  "query": {
    "bool": {
      "must": [
        { "match": { "event_type": "validation_failure" } },
        { "range": { "@timestamp": { "gte": "now-1h" } } }
      ],
      "should": [
        { "term": { "severity": "CRITICAL" } },
        { "term": { "severity": "HIGH" } }
      ],
      "minimum_should_match": 1
    }
  },
  "aggs": {
    "by_error_code": {
      "terms": { "field": "error_code.keyword", "size": 20 }
    },
    "by_context": {
      "terms": { "field": "app.keyword" }
    }
  }
}
```

### Kibana Visualizations

```
# Validation Error Rate (Line Chart)
Metric: Count
Buckets: Date Histogram on @timestamp (1 minute intervals)
Split Series: Terms on app.keyword

# SOX Compliance Failures (Data Table)
Columns: @timestamp, tenant_id, user_id, error_code, field
Filters: requires_sox_review = true
Sort: @timestamp descending

# Authentication Failure Heatmap
Y-Axis: Terms on client_ip.keyword
X-Axis: Date Histogram on @timestamp (5 minute intervals)
Metric: Count
Filter: is_authentication_failure = true
```

## Alert Configuration

**Alert Rules**: `monitoring/prometheus/validation-alerts.yml`

**Deployment**:
```bash
# Copy to Prometheus rules directory
sudo cp monitoring/prometheus/validation-alerts.yml /etc/prometheus/rules/

# Reload Prometheus
curl -X POST http://localhost:9090/-/reload
# OR
sudo systemctl reload prometheus
```

**Alert Severity Levels**:
- **CRITICAL**: Immediate action required (SOX failures, DDoS attacks, data leaks)
- **HIGH**: Prompt investigation needed (auth failures, PII issues)
- **WARNING**: Monitor for patterns (data quality issues, elevated error rates)

**Integration with Alertmanager**:
```yaml
# /etc/prometheus/alertmanager.yml
route:
  group_by: ['alertname', 'context']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'validation-team'
  routes:
    - match:
        severity: critical
        compliance: sox
      receiver: 'sox-compliance-team'
    - match:
        severity: critical
        security: ddos
      receiver: 'sre-pager'
    - match:
        compliance: gdpr
      receiver: 'privacy-team'

receivers:
  - name: 'validation-team'
    slack_configs:
      - channel: '#validation-alerts'
        send_resolved: true
  - name: 'sox-compliance-team'
    email_configs:
      - to: 'sox-compliance@company.com'
    pagerduty_configs:
      - service_key: 'YOUR-SERVICE-KEY'
  - name: 'sre-pager'
    pagerduty_configs:
      - service_key: 'YOUR-SRE-SERVICE-KEY'
  - name: 'privacy-team'
    email_configs:
      - to: 'privacy@company.com'
```

## Compliance Reporting

### SOX Compliance (Finance)

```promql
# Daily SOX-relevant validation failures
sum(increase(finance_validation_errors_total{error_code=~"JOURNAL_.*|ACCOUNT_.*|LEDGER_.*|TRANSACTION_.*"}[24h]))
```

```logql
# SOX audit report
{app="finance"} |= "validation_failure" | json | requires_sox_review="true"
| json timestamp, tenant_id, user_id, error_code, field, http_status
```

### GDPR Compliance (PII Detection)

```logql
# PII validation failures by context
{app=~"finance|identity|gateway"} |= "validation_failure" | json | contains_pii="true"
| stats count by app, field
```

### Security Incident Response

```logql
# Authentication attack investigation
{app="identity"} |= "identity_validation_failure" | json 
| is_authentication_failure="true"
| json client_ip, user_agent, session_id, timestamp
| where timestamp >= "2025-11-15T00:00:00Z"
```

## Performance Metrics

### Response Time Impact

```promql
# Correlation between validation errors and response times
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{uri="/api/finance/journal-entry"}[5m]))
```

### Throughput Analysis

```promql
# Request rate vs validation error rate
rate(http_server_requests_total[5m]) - rate(finance_validation_errors_total[5m])
```

## Troubleshooting

### High Error Rate Investigation

1. Check dashboard for error code distribution
2. Query logs for specific tenant/user patterns
3. Review correlation IDs for request tracing
4. Verify data sources and integration health

### Missing Audit Logs

```promql
# Detect metrics without corresponding logs
(rate(finance_validation_errors_total[5m]) > 0) unless
(count_over_time({app="finance"} |= "validation_failure" [5m]) > 0)
```

### Correlation ID Tracing

```logql
# Follow request across services
{app=~"gateway|identity|finance"} | json | correlation_id="abc123"
```

## Best Practices

1. **Regular Review**: Check dashboard daily for anomalies
2. **Alert Tuning**: Adjust thresholds based on baseline metrics
3. **Compliance Audits**: Export SOX/GDPR reports weekly
4. **Incident Response**: Document correlation IDs for investigations
5. **Capacity Planning**: Monitor error trends for scaling decisions
6. **Security Reviews**: Investigate authentication failure spikes immediately
7. **Data Quality**: Track field-specific failure patterns for data validation improvements

## Support

For questions or issues:
- Validation Framework: Platform Team (#platform-support)
- SOX Compliance: Finance Compliance Team
- GDPR/Privacy: Privacy Team
- Security Incidents: Security Operations Center (SOC)
