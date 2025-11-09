# CI/CD Pipeline Troubleshooting Guide

**Quick Reference for Common CI Issues**

---

## Network & Connectivity Issues

### ETIMEDOUT / ENETUNREACH Errors

**Symptoms:**
```
Error: connect ETIMEDOUT 104.16.72.101:443
Error: connect ENETUNREACH 2606:4700::6810:4965:443
```

**Root Cause:** Transient network failures connecting to Maven Central, Gradle Plugin Portal, or other repositories.

**Automatic Recovery:**
- ✅ Retry logic handles automatically (3 attempts with exponential backoff)
- ✅ Cache warmup job reduces likelihood by pre-fetching dependencies

**Manual Fix (if retry exhausted):**
1. Go to GitHub Actions → Failed workflow run
2. Click "Re-run failed jobs" or "Re-run all jobs"
3. Should succeed on retry (95% success rate)

**Prevention:**
- Cache warmup job already implemented
- Retry logic on all critical operations
- No action needed unless persistent failures

---

## Gradle Wrapper Issues

### Permission Denied (Exit 126)

**Symptoms:**
```
./gradlew: Permission denied
Process completed with exit code 126
```

**Root Cause:** gradlew file not executable in GitHub Actions environment.

**Status:** ✅ FIXED in all workflows (v2.0+)

**Verification:**
```yaml
# All workflows should have this step before any gradlew command
- name: Make gradlew executable
  run: chmod +x gradlew
```

**If still occurring:**
1. Check workflow has the chmod step
2. Verify gradlew file exists in repository
3. Check file is not corrupted

---

## Build Failures

### Log Gate Failures

**Symptoms:**
```
[ERROR] Unexpected error patterns found in build output
FAILED: Log gate detected 3 unexpected errors
```

**Root Cause:** Build logs contain ERROR or Exception patterns not in allowlist.

**Investigation Steps:**
1. Review the full build log for ERROR/Exception lines
2. Determine if errors are expected (legitimate) or actual issues

**Resolution A - Expected Errors (add to allowlist):**
```bash
# Edit scripts/ci/error-allowlist.txt
echo "YOUR_EXPECTED_ERROR_PATTERN" >> scripts/ci/error-allowlist.txt
git add scripts/ci/error-allowlist.txt
git commit -m "chore: add expected error pattern to log gate allowlist"
```

**Resolution B - Actual Bug:**
1. Fix the code causing the error
2. Verify locally: `./gradlew build`
3. Commit fix and push

**Current Allowlist Patterns:**
- `AUTHENTICATION_FAILED` - Expected auth test failures
- `TENANT_SLUG_EXISTS` - Expected tenant validation errors

---

## Cache Issues

### Cache Not Being Used

**Symptoms:**
- Every build downloads dependencies from scratch
- Build times consistently long (~30min+)
- "Downloading" messages in logs

**Root Cause:** Cache not warming up or not sharing properly.

**Check:**
1. Verify cache-warmup job succeeded
2. Check `actions/setup-java` has `cache: gradle` parameter
3. Ensure downstream jobs have `needs: cache-warmup`

**Fix:**
```yaml
# Verify in each job:
- name: Set up JDK
  uses: actions/setup-java@v4
  with:
    java-version: ${{ env.JAVA_VERSION }}
    distribution: ${{ env.JAVA_DISTRIBUTION }}
    cache: gradle  # ← This must be present
```

---

## Test Failures

### Flaky Integration Tests

**Symptoms:**
- Tests pass locally but fail in CI
- Tests fail intermittently (not deterministic)
- PostgreSQL connection issues

**Common Causes:**
1. **Service not ready:** PostgreSQL/Kafka not fully started
2. **Port conflicts:** Services competing for ports
3. **Timing issues:** Tests expecting immediate readiness

**Fix for Service Readiness:**
```yaml
# Already implemented in ci.yml:
services:
  postgres:
    options: >-
      --health-cmd pg_isready
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5  # ← Waits until healthy
```

**Debug Steps:**
1. Check service container logs in GitHub Actions
2. Verify service ports are correctly mapped
3. Check connection strings in test configuration
4. Add wait/retry logic in test setup if needed

---

## Timeout Issues

### Job Timeout Exceeded

**Symptoms:**
```
Error: The operation was canceled.
Job timeout exceeded (30 minutes)
```

**Current Timeouts:**
- cache-warmup: 10 minutes
- lint: 10 minutes
- build: 30 minutes
- integration-tests: 20 minutes
- architecture: 15 minutes
- security: 10 minutes

**Resolution:**
1. Check if timeout is legitimate (hung process) or job needs more time
2. Review build logs for stuck operations
3. If legitimate increase needed, adjust workflow:

```yaml
jobs:
  build:
    timeout-minutes: 35  # Increase if consistently timing out
```

---

## Action Version Issues

### Deprecated Action Warnings

**Symptoms:**
```
Warning: The `set-output` command is deprecated and will be disabled soon.
Warning: gradle/wrapper-validation-action uses deprecated functionality
```

**Status:** ✅ All actions pinned to latest versions (v3.0)

**If new warnings appear:**
1. Check GitHub Actions UI for deprecation notices
2. Update to latest version of the action
3. Verify in documentation: [GITHUB_ACTIONS_UPGRADE.md](./GITHUB_ACTIONS_UPGRADE.md)

**Current Action Versions:**
- `actions/checkout@v4`
- `actions/setup-java@v4`
- `actions/upload-artifact@v4`
- `gradle/actions/wrapper-validation@v3.5.0`
- `gradle/actions/setup-gradle@v3.5.0`
- `nick-fields/retry@v3.0.0`
- `aquasecurity/trivy-action@0.28.0`

---

## Configuration Cache Issues

### Configuration Cache Disabled

**Symptoms:**
```
Configuration cache is not enabled
To enable, add to GRADLE_OPTS: -Dorg.gradle.configuration-cache=true
```

**Status:** ✅ ENABLED in ci.yml (v2.0+)

**Verification:**
```yaml
env:
  GRADLE_OPTS: '-Dorg.gradle.configuration-cache=true -Dorg.gradle.daemon=false -Dorg.gradle.parallel=true'
```

**If disabled:**
- Check env section in workflow file
- Verify GRADLE_OPTS includes configuration-cache flag
- Rebuild to regenerate cache

---

## Artifact Upload Failures

### No Files Found

**Symptoms:**
```
Warning: No files were found with the provided path: **/build/reports/ktlint/**
No artifacts will be uploaded
```

**Root Cause:** 
1. Path pattern incorrect
2. Build didn't generate expected files
3. Upload condition wrong

**Fix:**
```yaml
# Verify upload step:
- name: Upload ktlint reports
  if: failure()  # ← Only upload on failure for error reports
  uses: actions/upload-artifact@v4
  with:
    name: ktlint-reports
    path: '**/build/reports/ktlint/**'  # ← Verify path matches output
```

---

## Security Scan Issues

### Trivy Scan Failures

**Symptoms:**
```
Error: failed to scan: error occurred during scanning
```

**Common Causes:**
1. Network timeout downloading vulnerability database
2. SARIF upload permission issue
3. Large repository causing OOM

**Fix:**
```yaml
# Verify permissions in workflow:
permissions:
  contents: read
  security-events: write  # ← Required for SARIF upload
```

**If persistent:**
1. Check Trivy version is pinned: `@0.28.0`
2. Review GitHub Security tab for more details
3. Consider excluding large binary files

---

## Concurrency Issues

### Jobs Not Canceling

**Symptoms:**
- Multiple runs for same PR continue even after new push
- Wasted resources on obsolete runs

**Status:** ✅ ENABLED in all workflows (v2.0+)

**Verification:**
```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true  # ← Must be true
```

---

## Performance Debugging

### Slow Build Times

**Expected Times (v3.0):**
- Cold run: ~30-32 minutes
- Warm run: ~28-30 minutes

**If consistently slower:**
1. Check cache-warmup job is succeeding
2. Verify configuration cache is enabled
3. Review Gradle build scan for bottlenecks
4. Check network latency to GitHub Actions

**Optimization Checklist:**
- [ ] Cache warmup job running first
- [ ] Configuration cache enabled
- [ ] Test duplication eliminated
- [ ] Parallel jobs executing
- [ ] Retry logic not exhausting (check logs)

---

## Emergency Procedures

### CI Completely Broken

**Immediate Actions:**
1. Check GitHub Status: https://www.githubstatus.com/
2. Review recent commits for workflow changes
3. Revert to last known good commit if needed
4. Disable workflows temporarily if critical

**Rollback Command:**
```bash
# Revert to previous working commit
git revert <commit-hash>
git push

# Or temporarily disable workflow
# Edit .github/workflows/ci.yml and add:
# on: [workflow_dispatch]  # Manual only
```

---

## Getting Help

### Information to Collect

When reporting CI issues, include:
1. Workflow run URL
2. Job name that failed
3. Error message (full output)
4. Recent changes (commits)
5. Whether issue is reproducible locally

### Useful Commands

```bash
# Run CI checks locally
./gradlew ktlintCheck check build

# Check for build issues
./gradlew build --stacktrace --info

# Test with fresh cache
./gradlew clean build --no-build-cache

# Check dependency resolution
./gradlew dependencies

# Verify wrapper
./gradlew wrapper --gradle-version=9.0.0
```

---

## Quick Decision Tree

```
CI Failed?
├─ Network timeout? → Retry workflow (auto-handled)
├─ Log gate failure? → Check if expected → Add to allowlist OR fix bug
├─ Test failure? → Run locally → Fix issue → Push
├─ Permission denied? → Check chmod +x gradlew step exists
├─ Cache miss? → Check cache-warmup job → Verify cache: gradle
├─ Timeout? → Review logs → Adjust timeout if legitimate
└─ Unknown? → Collect logs → Check changelog → Report issue
```

---

**Last Updated:** 2025-11-09  
**Version:** 3.0  
**Maintainer:** Development Team
