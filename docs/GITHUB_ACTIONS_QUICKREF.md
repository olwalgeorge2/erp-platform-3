# GitHub Actions Quick Reference

## Upgraded Workflows (v2.0 - Industry Standard)

### 📋 All Workflows Include:
- ✅ Concurrency controls (auto-cancel redundant runs)
- ✅ gradle/actions/setup-gradle@v3 (optimal caching)
- ✅ Proper permissions (least privilege)
- ✅ Latest action versions (@v4)
- ✅ Timeout limits
- ✅ Error reporting & artifacts

---

## Workflow Matrix

| Workflow | Trigger | Duration | Key Features |
|----------|---------|----------|--------------|
| **ci.yml** | PR/Push | ~30min | Parallel jobs, security scan, integration tests |
| **lint.yml** | PR/Push | ~10min | ktlint checks, auto-upload reports |
| **arch-governance.yml** | Weekly Mon 9AM | ~15min | ArchUnit tests, artifact retention 30d |
| **nightly.yml** | Daily 2AM | ~30min | Build scan, Trivy security, coverage |
| **governance-audit.yml** | Weekly Mon 9AM | ~15min | Platform-shared audit, auto-issue creation |
| **smoke.yml** | Manual | ~15min | Smoke tests with env inputs |

---

## CI Pipeline Jobs

```
┌─────────┐
│ PR/Push │
└────┬────┘
     │
     ├──────────────────────┐
     │                      │
     ▼                      ▼
┌────────┐          ┌──────────────┐
│  Lint  │          │ Architecture │
│ 10min  │          │    15min     │
└───┬────┘          └──────────────┘
    │
    ▼
┌────────┐
│ Build  │
│ 30min  │
└───┬────┘
    │
    ├──────────────┐
    ▼              ▼
┌──────────┐  ┌──────────┐
│Integration│  │ Security │
│   20min   │  │  10min   │
└──────────┘  └──────────┘
```

**Total Time:** ~30min (parallel execution)  
**Sequential Would Be:** ~105min  
**Savings:** ~70% faster

---

## Key Improvements

### 🔒 Security
- Trivy vulnerability scanning → GitHub Security tab
- SARIF format for integrated security insights
- Least privilege permissions on all workflows
- Gradle wrapper validation

### ⚡ Performance
- 30-50% faster builds (setup-gradle@v3 caching)
- Parallel job execution (lint + arch simultaneously)
- Concurrency auto-cancel saves compute costs

### 📊 Observability
- All test results uploaded as artifacts
- Coverage reports (Kover HTML + XML)
- Build scan reports
- 90-day governance audit retention

### 🛠️ Maintainability
- Consistent structure across all workflows
- Centralized Java version (env.JAVA_VERSION)
- Modern action versions
- Clear job naming

---

## Common Commands

### Local Pre-Push Checks
```bash
# Full verification (matches CI)
just verify

# Quick checks
just lint
just arch-tests
just test
```

### Workflow Dispatch
```bash
# Trigger nightly build manually
gh workflow run nightly.yml

# Run smoke tests
gh workflow run smoke.yml

# Architecture governance
gh workflow run arch-governance.yml
```

### Monitoring
```bash
# Check latest CI run
gh run list --workflow=ci.yml --limit 5

# View specific run
gh run view <run-id>

# Download artifacts
gh run download <run-id>
```

---

## Artifact Retention Policies

| Artifact Type | Retention | Workflows |
|---------------|-----------|-----------|
| Test results | 7 days | ci.yml, lint.yml |
| Integration test results | 7 days | ci.yml |
| Build artifacts | 30 days | ci.yml |
| Architecture reports | 30 days | arch-governance.yml, ci.yml |
| Coverage reports | 30 days | nightly.yml |
| Audit reports | 90 days | governance-audit.yml |
| Smoke test results | 14 days | smoke.yml |

---

## Security Scanning

**Trivy** runs on:
- Every PR/push (ci.yml security job)
- Nightly builds (nightly.yml dependency-scan)

**Results:** GitHub Security tab → Code scanning alerts

**View:** Repository → Security → Code scanning

---

## Branch Protection Recommendations

```yaml
# Suggested branch protection for main/develop
required_status_checks:
  - Lint
  - Build & Unit Tests
  - Integration Tests
  - Architecture Tests

required_reviews: 1
dismiss_stale_reviews: true
require_code_owner_reviews: false
```

---

## Troubleshooting

### CI Failing?
1. Check ktlint: `just lint`
2. Check arch tests: `just arch-tests`
3. Check build: `just build`
4. Check integration: Docker running? `just test-identity-infra`

### Concurrency Issues?
- Workflows auto-cancel on new push
- Check: Actions → Running workflows
- Cancel manually: Click "Cancel workflow"

### Caching Issues?
- setup-gradle@v3 handles cache automatically
- Clear cache: Settings → Actions → Caches
- Force rebuild: Re-run with "Re-run all jobs"

---

## Next Steps After First Run

1. ✅ Verify parallel execution works
2. ✅ Check Gradle cache effectiveness (build times)
3. ✅ Review Security tab for Trivy results
4. ✅ Confirm artifacts uploaded correctly
5. ✅ Test concurrency cancellation (push 2x rapidly)

---

For detailed upgrade notes, see: [GITHUB_ACTIONS_UPGRADE.md](./GITHUB_ACTIONS_UPGRADE.md)
