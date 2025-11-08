# Architecture Testing Guide (ADR-006)

This guide explains how to run and evolve our architecture tests. It complements ADR‑006 (Platform‑Shared Governance) and our Sprint 2 remediation plan.

## Quick Start (Enforced)

- Run governance tests (no opt‑in required):
  - `./gradlew :tests:arch:test --tests "*PlatformSharedGovernanceRules*"`
- View reports:
  - HTML: `tests/arch/build/reports/tests/test/index.html`
  - XML:  `tests/arch/build/test-results/test/TEST-*.xml`
- Capture/update baseline (optional, advisory):
  - `./gradlew :tests:arch:archFreezeBaseline`
  - Baseline files: `tests/arch/archunit-freeze/`

## What The Rules Check

Current rule suite: `tests/arch/src/test/kotlin/com/erp/tests/arch/PlatformSharedGovernanceRules.kt`

- Platform‑shared purity (shared must not contain):
  - REST resources (`jakarta.ws.rs.Path`)
  - JPA entities (`jakarta.persistence.Entity`/`MappedSuperclass`)
  - Quarkus CDI services (`jakarta.enterprise.context.*`)
  - Direct dependencies on platform‑infrastructure
- Cross‑context decoupling:
  - Identity/Finance/Commerce must not depend on other bounded contexts directly
  - Communication via events/integration boundaries (ADR‑003)

## Classpath Wiring (Important)

ArchUnit imports classes from the test runtime classpath of `:tests:arch`.
We wire the modules under test via `testRuntimeOnly` in `tests/arch/build.gradle.kts`:

- Platform‑shared: `common-types`, `common-security`, `common-observability`, `common-messaging`
- Tenancy‑identity: `identity-domain`, `identity-application`, `identity-infrastructure`

To expand coverage to another context (e.g., commerce), add:

```
// tests/arch/build.gradle.kts
dependencies {
  testRuntimeOnly(project(":bounded-contexts:commerce:commerce-ecommerce:ecommerce-domain"))
  testRuntimeOnly(project(":bounded-contexts:commerce:commerce-ecommerce:ecommerce-application"))
  testRuntimeOnly(project(":bounded-contexts:commerce:commerce-ecommerce:ecommerce-infrastructure"))
}
```

Then remove any `allowEmptyShould(true)` guards for that context’s rule.

## Handling “Empty Should” (No Matches)

When the classpath doesn’t contain a given context, a rule might match no classes. We intentionally allow empty rules for unwired contexts using `rule.allowEmptyShould(true)` to keep checks advisory while we expand coverage incrementally.

- Prefer method‑level guard (local) over global config while we scale coverage.
- Once a context is wired, remove `allowEmptyShould(true)` for that context.

## Baseline Freeze (Optional, Advisory)

- Config: `tests/arch/src/test/resources/archunit.properties`
  - `archunit.freeze.store.default.path=archunit-freeze`
  - `archunit.freeze.store.default.allowStoreCreation=true`
  - `archunit.freeze.refreeze=true`
- Generate/update baseline:
  - `./gradlew :tests:arch:archFreezeBaseline`
- Commit changes in `tests/arch/archunit-freeze/` if you want a shared baseline.
- Use baselines when you want to prevent regressions while cleaning up legacy violations.

## CI Integration (Enforced)

- Main CI enforces and blocks on:
  - `PlatformSharedGovernanceRules`
  - `LayeringRules`
  - `HexagonalArchitectureRules`
- Weekly governance workflow also runs the same suites on schedule.

## Troubleshooting

- “No tests found for given includes”: run module‑scoped tests as above; don’t run `--tests` at root for arch tests.
- “Rule failed to check any classes”: wire the context under test via `testRuntimeOnly`, or use `allowEmptyShould(true)` until wired.
- Baseline not updating: ensure `archunit.freeze.*` properties are present; use the `archFreezeBaseline` task.
- Slow runs: start with fewer contexts wired; expand incrementally per Sprint plan.

## Contributor Workflow

1) Run ktlint/lint + tests as usual
2) Run architecture tests locally before PR:
   - `./gradlew :tests:arch:test`
3) If failures:
   - Inspect report (HTML) and follow ADR-006 guidance
   - For platform-shared issues, move code to proper layer (infra vs shared)
   - For cross-context issues, introduce an integration port/event instead of direct import
4) Commit, push, and ensure CI gates (build + architecture-tests) are green

## Pre-commit Hook (Optional but Recommended)

Automate local checks before commits using the provided hook.

- Hook script: `scripts/hooks/pre-commit`
- Enable for your repo:
  - `git config core.hooksPath scripts/hooks`
- Windows note: use Git Bash or WSL for shell execution

## References

- ADR‑006 Platform‑Shared Governance Rules: `docs/adr/ADR-006-platform-shared-governance.md`
- Sprint 2 Remediation Plan: `docs/SPRINT2_ARCH_REMEDIATION.md`
- ArchUnit: https://www.archunit.org/
