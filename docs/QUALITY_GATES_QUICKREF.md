# 🚦 Quality Gates Quick Reference

## Installation (Once)
```powershell
just install-hooks
```

## Clean Gates Enforced

### Pre-Commit (Automatic on `git commit`)
- ✅ **ktlint** (if Kotlin files changed)
- ✅ **Architecture tests** (if code files changed)

### Pre-Push (Automatic on `git push`)
- ✅ **ktlint** (full codebase)
- ✅ **Architecture tests** (full suite)
- ✅ **Identity infrastructure tests**

## Daily Commands

### Fast Iteration
```powershell
just lint          # Style check only (~10s)
just format        # Auto-fix style issues
```

### Before Committing
```powershell
just verify        # Full local preflight (~1-2 min)
```

### Before Pushing
```powershell
just preflight     # Format + verify combo
just push          # Explicit guarded push
```

## Bypass (Emergency Only)
```powershell
# Skip pre-commit
$env:SKIP_ARCH_HOOK='1'; git commit -m "message"

# Skip pre-push
$env:SKIP_PREPUSH_CHECKS='1'; git push
```

## Common Failures

### ktlint Failed
```powershell
just format        # Auto-fix
git commit --amend --no-edit
```

### Architecture Test Failed
```powershell
# Review violation in output
code docs/ARCHITECTURE.md
# Fix the violation
just arch          # Re-test
```

### Hook Not Running
```powershell
just install-hooks # Reinstall
```

## Full Documentation
📖 **[docs/LOCAL_QUALITY_GATES.md](LOCAL_QUALITY_GATES.md)** - Complete guide with troubleshooting

## Tighten Further?

Want stricter gates? Options available:
- ✅ Reduce line length (100 or 80)
- ✅ Add smoke tests to pre-push
- ✅ Enforce code coverage thresholds
- ✅ Add domain-specific architecture rules
- ✅ Enable additional ktlint rules

Just ask! 🚀
