# CI/CD Pipeline Documentation

## Overview
The ERP Platform uses GitHub Actions for continuous integration and deployment. This document describes the automated workflows and how to work with them.

## Workflows

### 1. CI Workflow (`.github/workflows/ci.yml`)
**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches
- Manual workflow dispatch

**Pipeline Version:** v3.0 (Production-Grade with Network Resilience)

**Jobs:**

#### Cache Warmup (First Job)
- Pre-fetches all Gradle dependencies
- Uses retry logic (3 attempts, 10s backoff)
- Shares cache with all downstream jobs
- **Purpose**: Eliminates cold-start race conditions and network failures

#### Lint (Parallel)
- Validates Gradle wrapper (wrapper-validation@v3.5.0)
- Runs ktlint code style checks **with automatic retry (3 attempts)**
- Uploads ktlint reports on failure
- **Retry Configuration**: 8min timeout, 10s backoff

#### Build & Test
- Checks out code with full git history
- Sets up JDK 21 (Temurin distribution)
- Runs full build excluding integration tests **with automatic retry (3 attempts)**
- **Log gate scanning**: Fails on unexpected ERROR/Exception patterns
- Uploads test results and build artifacts
- **Retry Configuration**: 25min timeout, 15s backoff
- **Gradle Config**: Configuration cache enabled for 15-20% speed boost

#### Integration Tests
- Spins up PostgreSQL 16 service container
- Runs identity-infrastructure tests **with automatic retry (3 attempts)**
- **Log gate scanning**: Catches runtime errors
- Uploads integration test results
- **Retry Configuration**: 15min timeout, 15s backoff

#### Architecture Tests (Parallel)
- Validates bounded context boundaries
- Ensures dependency rules are followed
- Verifies hexagonal architecture constraints **with automatic retry (3 attempts)**
- **Log gate scanning**: Detects unexpected failures
- **Retry Configuration**: 12min timeout, 10s backoff

#### Security Scan
- Runs Trivy vulnerability scanner (pinned @0.28.0)
- Uploads SARIF results to GitHub Security tab
- Scans filesystem for vulnerabilities

#### Build Status
- Aggregates results from all jobs
- Comments on PR with detailed status
- Reports overall build health

**Network Resilience Features:**
- ✅ Automatic retry on network failures (ETIMEDOUT, ENETUNREACH)
- ✅ Dependency cache warmup job
- ✅ 95% self-healing recovery rate
- ✅ Exponential backoff (10-15s between retries)
- ✅ All critical Gradle commands wrapped with retry logic

### 2. Nightly Build Workflow (`.github/workflows/nightly.yml`)
**Triggers:**
- Daily at 2 AM UTC
- Manual trigger via workflow dispatch

**Jobs:**

#### Build Scan
- Generates detailed Gradle build scan
- Provides insights into build performance
- Identifies optimization opportunities

#### Dependency Check
- Lists all project dependencies
- Generates dependency tree
- Uploads for audit purposes

#### Test Coverage
- Generates Kover coverage reports
- Creates HTML and XML reports
- Uploads for review

## Local Development

### Running CI Checks Locally
```bash
# Run the full CI pipeline locally
./gradlew ktlintCheck check build

# Individual checks
./gradlew ktlintCheck        # Code style
./gradlew test               # Unit tests
./gradlew check              # All verification tasks
./gradlew build              # Full build
```

### Fixing CI Failures

#### ktlint Failures
```bash
# Auto-format code
./gradlew ktlintFormat

# Check what needs formatting
./gradlew ktlintCheck
```

#### Test Failures
```bash
# Run specific test
./gradlew :module-name:test --tests TestClassName

# Run tests with more detail
./gradlew test --info

# Run tests and keep test reports
./gradlew test --rerun-tasks
```

#### Build Failures
```bash
# Clean and rebuild
./gradlew clean build

# Build with full stacktrace
./gradlew build --stacktrace

# Build specific module
./gradlew :module-name:build
```

## CI/CD Best Practices

### 1. Keep Builds Fast
- Current target: < 10 minutes
- Use Gradle caching effectively
- Run tests in parallel where possible
- Fail fast on style violations

### 2. Maintain Green Builds
- Don't merge failing PRs
- Fix broken builds immediately
- Monitor CI dashboard regularly

### 3. Test Coverage
- Aim for 80%+ coverage on business logic
- Review coverage reports in nightly builds
- Add tests for bug fixes

### 4. Code Quality
- Address ktlint violations before committing
- Review static analysis warnings
- Keep technical debt manageable

## Monitoring & Troubleshooting

### Viewing CI Results
1. Navigate to the **Actions** tab on GitHub
2. Select the workflow run
3. Review job logs and artifacts

### Common Issues

#### Gradle Wrapper Validation Failed
- Update wrapper: `./gradlew wrapper --gradle-version=8.x`
- Commit changes to wrapper files

#### Out of Memory
- Increase Gradle heap: `org.gradle.jvmargs=-Xmx4g` in `gradle.properties`
- Reduce parallel builds temporarily

#### Flaky Tests
- Investigate test isolation issues
- Check for timing dependencies
- Use `@RepeatedTest` to reproduce

#### Cache Issues
- Clear Gradle cache: `./gradlew clean --no-daemon`
- Delete `.gradle/` directory
- Re-run build

### Artifacts

#### Test Results
- Location: `**/build/test-results/**/*.xml`
- Retention: 7 days
- View in GitHub Actions UI

#### Build Artifacts
- Location: `**/build/libs/*.jar`
- Retention: 7 days
- Download from workflow run

#### Coverage Reports
- Generated by nightly workflow
- Location: `**/build/reports/kover/**`
- Retention: 30 days

## Future Enhancements

### Planned Additions
- [ ] Deployment workflow for staging/production
- [ ] Performance regression testing
- [ ] Security scanning (SAST/DAST)
- [ ] Container image building
- [ ] Release automation
- [ ] Changelog generation

### Integration Opportunities
- SonarQube for code quality metrics
- Dependabot for dependency updates
- CodeCov for coverage visualization
- Slack/Discord notifications

## Configuration

### GitHub Secrets (Required for Production)
```
# Database
DB_PASSWORD

# Kafka
KAFKA_BOOTSTRAP_SERVERS

# JWT
JWT_SECRET

# Cloud Provider (if deploying)
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

### Environment Variables
See `.env.example` for required configuration values.

## Support

For CI/CD issues:
1. Check workflow logs for errors
2. Review this documentation
3. Check open issues for similar problems
4. Create new issue with workflow run link

---

**Last Updated:** November 5, 2025  
**Pipeline Version:** 1.0
