# Phase 4C – Cache Benchmarking & Warmup Evidence

**Execution date:** 2025-11-17  
**Environment:** Finance QA cluster  
**Load Tool:** k6 (script: `scripts/validation/cache-benchmark.js`)  
**Dataset:** 50k journal entry posts, 15k AP invoices, 12k AR open items (3 tenants)

## 1. Warmup Results

| Cache | Warmup Source | Records Loaded | Duration |
|-------|---------------|----------------|----------|
| LedgerExistenceCache | `LedgerRepository.findRecent(50)` | 48 | 1.2s |
| ChartOfAccountsCache | `ChartOfAccountsRepository.findRecent(25)` | 25 | 2.8s |
| VendorExistenceCache (AP) | Vendor table (`TOP 100`) | 100 | 0.9s |
| CustomerExistenceCache (AR) | Customer table (`TOP 100`) | 100 | 1.0s |

Warmup logs stored at `logs/finance-accounting/cache-warmup-2025-11-17.log`.

## 2. Hit Rate & Latency

| Scenario | Metric | Cold Run | Warm Run | Improvement |
|----------|--------|----------|----------|-------------|
| Journal entry validation (accounting) | `validation.cache.hitratio{cache="ledger-existence"}` | 0.07 | **0.91** | +84 pp |
| Journal entry validation | `validation.cache.hitratio{cache="chart-of-accounts"}` | 0.05 | **0.88** | +83 pp |
| AP vendor validation | `validation.cache.hitratio{cache="vendor-existence"}` | 0.63 | **0.92** | +29 pp |
| AR customer validation | `validation.cache.hitratio{cache="customer-existence"}` | 0.61 | **0.90** | +29 pp |
| Accounting validation latency | `p95 validation.rule.duration_seconds` | 118 ms | **46 ms** | 61% faster |
| AP invoice validation latency | `p95 validation.request.duration_seconds{context="finance-ap"}` | 212 ms | **95 ms** | 55% faster |

Prometheus queries attached at `reports/validation/phase4c/prometheus-cache-results.txt`.

## 3. Load Test Notes

- Journals posted at 150 req/s for 10 minutes using 3 tenants.
- Cache miss spikes only during first 30 seconds (expected warmup window).
- No stale data observed; posting a new chart/account invalidated caches (`chartCache.put/evict` logs).

## 4. Acceptance Criteria Mapping

| Requirement | Evidence |
|-------------|----------|
| Cache hit rate > 80% | See table above (all contexts ≥ 88%). |
| Latency reduced by >50% | Accounting/AP metrics show >55% improvement. |
| Cache warming implemented | `AccountingCacheWarmer` logs + warmup table. |
| Benchmark artifacts | Stored under `reports/validation/phase4c/`. |

## 5. Next Steps

- Re-run benchmark monthly.
- Consider cross-tenant warming once production traffic baseline is collected.
