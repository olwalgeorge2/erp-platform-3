# Local Development Clean Gates Guide

This guide explains the quality gates enforced locally to catch issues before they reach CI/CD.

## 🚦 Quality Gates Overview

### Pre-Commit Hook
**Runs automatically on `git commit`**
- ✅ **ktlint** - Kotlin code style validation (if `.kt` or `.kts` files changed)
- ✅ **Architecture Tests** - ADR-006, layering, hexagonal architecture enforcement

### Pre-Push Hook
**Runs automatically on `git push`**
- ✅ **ktlint** - Full codebase style check
- ✅ **Architecture Tests** - Full ArchUnit test suite
- ✅ **Identity Infrastructure Tests** - Critical domain tests

---

## 🛠️ Quick Commands

### One-Time Setup
```powershell
# Install git hooks (required once per clone)
just install-hooks
```

### Daily Development

| Command | Purpose | When to Use |
|---------|---------|-------------|
| `just lint` | Check code style only | Before committing Kotlin changes |
| `just format` | Auto-fix code style | When ktlint reports violations |
| `just verify` | Full local preflight | Before pushing (recommended) |
| `just preflight` | Format + verify | Before creating PR |
| `just push` | Guarded push | Push with explicit pre-push validation |

### Manual Hook Testing
```powershell
# Test pre-commit checks manually
just pre-commit

# Test pre-push checks manually
just pre-push
```

---

## 📋 Detailed Gate Breakdown

### 1. Lint Only (Fast - ~10-30s)
```powershell
./gradlew ktlintCheck
```
**What it checks:**
- 120-char line length
- No wildcard imports
- Trailing commas on multi-line declarations
- Consistent indentation (4 spaces)
- All rules in `.editorconfig`

**When it fails:**
```powershell
# Auto-fix most issues
./gradlew ktlintFormat

# Then review changes and commit
git add .
git commit
```

### 2. Full Preflight (Medium - ~1-2 min)
```powershell
./gradlew verifyLocal
# OR
just verify
```
**What it runs:**
- `ktlintCheck` on all Kotlin code
- `:tests:arch:test` - Architecture governance
- `:bounded-contexts:tenancy-identity:identity-infrastructure:test` - Critical domain tests

**Use before:**
- Creating pull requests
- Pushing to shared branches
- After refactoring sessions

### 3. Guarded Push (Built-in)
```powershell
git push
# OR
just push
```
**Automatic blocking:**
- Pre-push hook runs before push completes
- Push is **aborted** if any check fails
- No broken code reaches remote branches

---

## 🔧 Hook Management

### Install Hooks
```powershell
# Copy hooks from scripts/hooks/ to .git/hooks/
just install-hooks
```

### Temporarily Bypass (Emergency Only)
```powershell
# Skip pre-commit checks (single commit)
$env:SKIP_ARCH_HOOK='1'; git commit -m "emergency fix"

# Skip pre-push checks (single push)
$env:SKIP_PREPUSH_CHECKS='1'; git push
```

### Uninstall Hooks
```powershell
just uninstall-hooks
```

---

## 🎯 Recommended Workflow

### Standard Commit Flow
```powershell
# 1. Make changes
code src/something.kt

# 2. Auto-format if needed
just format

# 3. Commit (pre-commit hook auto-runs)
git commit -m "feat: add new feature"
# → Hook runs: ktlint + arch tests

# 4. Push (pre-push hook auto-runs)
git push
# → Hook runs: ktlint + arch tests + identity tests
```

### Quick Iteration Flow
```powershell
# Work on feature
code src/feature.kt

# Fast check during development
just lint

# Fix style issues
just format

# Verify before committing
just verify

# Commit with confidence
git commit -m "feat: implement feature"
```

---

## 🚨 Troubleshooting

### ktlint Failures

**Problem:** `Lint failed, found 23 violations`
```powershell
# Solution: Auto-fix
just format
# Review changes
git diff
# Commit
git commit --amend --no-edit
```

**Problem:** `Max line length exceeded (line 45, column 121)`
```powershell
# Solution: Break long lines manually or suppress
# Option 1: Break the line
val result = service
    .performLongOperation()
    .withParameter()

# Option 2: Suppress (rare, needs justification)
@Suppress("MaxLineLength")
```

### Architecture Test Failures

**Problem:** `Layer violation: Application layer accessed Infrastructure directly`
```powershell
# Solution: Review architecture docs
code docs/ARCHITECTURE.md

# Fix the violation (use interfaces/ports)
# Then run tests
just arch
```

### Hook Not Running

**Problem:** Commit succeeds without running checks
```powershell
# Reinstall hooks
just install-hooks

# Verify installation
ls .git/hooks/pre-commit
ls .git/hooks/pre-push
```

---

## 🎚️ Strictness Levels

### Current Configuration

| Aspect | Level | Enforcement |
|--------|-------|-------------|
| **ktlint failures** | 🔴 **STRICT** | `ignoreFailures = false` - Build fails |
| **Line length** | 🟡 **MODERATE** | 120 chars (reasonable for modern screens) |
| **Architecture rules** | 🔴 **STRICT** | ArchUnit failures block commits |
| **Wildcard imports** | 🔴 **STRICT** | Enforced by ktlint |
| **Filename rules** | 🟢 **RELAXED** | Disabled (pragmatic) |

### Tightening Options (Not Yet Applied)

If you want even stricter gates, we can:

1. **Reduce Line Length**
   ```kotlin
   // From 120 → 100 or 80
   max_line_length = 100
   ```

2. **Add More Architecture Rules**
   ```kotlin
   // Example: No direct DB access from API layer
   // Example: All DTOs must be in 'api' package
   ```

3. **Enable Additional ktlint Rules**
   ```properties
   # Enforce specific naming conventions
   ktlint_standard_function_naming = enabled
   ktlint_standard_property_naming = enabled
   ```

4. **Add Smoke Tests to Pre-Push**
   ```powershell
   # Current: pre-push runs unit tests only
   # Option: Add smoke tests (requires local Docker)
   just smoke
   ```

5. **Add Code Coverage Gates**
   ```kotlin
   // Fail if coverage drops below threshold
   kover {
       verify {
           rule {
               minBound(80) // 80% coverage required
           }
       }
   }
   ```

---

## 📊 Performance Impact

| Gate | Time | Frequency | Skippable |
|------|------|-----------|-----------|
| ktlint | ~10-30s | Every Kotlin file change | Via `$env:SKIP_ARCH_HOOK='1'` |
| Arch tests | ~30-60s | Every code commit | Via `$env:SKIP_ARCH_HOOK='1'` |
| Identity tests | ~20-40s | Every push | Via `$env:SKIP_PREPUSH_CHECKS='1'` |
| **Total (worst case)** | ~2 min | Per push | Emergency bypass available |

---

## 🎯 Next Steps

### If You Want Stricter Gates

**Option 1: Tighter Line Length**
```powershell
# Edit .editorconfig
max_line_length = 100  # from 120
```

**Option 2: More Architecture Rules**
- Define additional domain-specific rules in `tests/arch/`
- Add allowlist validation for domain events
- Enforce specific package structures

**Option 3: Add Health Checks to Smoke**
```powershell
# Modify justfile to include health validation
just smoke-with-health
```

**Option 4: Coverage Enforcement**
```powershell
# Add to verifyLocal task
./gradlew koverVerify  # Fails if coverage < threshold
```

### If You Want Faster Iteration

**Option 1: Lighter Pre-Commit**
```powershell
# Remove arch tests from pre-commit
# Keep only ktlint for fast feedback
```

**Option 2: Parallel Test Execution**
```kotlin
// Enable in gradle.properties
org.gradle.parallel=true
org.gradle.workers.max=4
```

---

## 📚 Reference

- **ktlint config:** `.editorconfig`
- **Hook scripts:** `scripts/hooks/`
- **Architecture rules:** `tests/arch/src/test/kotlin/`
- **Gradle tasks:** `build.gradle.kts` (root)
- **Just recipes:** `justfile`

---

## ✅ Verification Checklist

Before considering your local environment production-ready:

- [ ] Hooks installed: `just install-hooks`
- [ ] Hooks execute: Commit and see `[pre-commit]` output
- [ ] ktlint passes: `just lint` returns 0 violations
- [ ] Format works: `just format` fixes issues
- [ ] Verify succeeds: `just verify` all green
- [ ] Push blocked on failure: Break a test, try pushing (should fail)
- [ ] Emergency bypass works: `$env:SKIP_PREPUSH_CHECKS='1'; git push`

---

**Need tighter gates?** Just say the word:
- Stricter line length (100 or 80)
- Additional architecture rules
- Coverage thresholds
- Smoke test health checks in pre-push
- Custom domain-specific validations
