# Documentation Index

**ERP Platform Documentation Hub**

---

## 📋 Quick Navigation

### CI/CD Pipeline Documentation

| Document | Purpose | When to Use |
|----------|---------|-------------|
| [CI Evolution Changelog](./CI_EVOLUTION_CHANGELOG.md) | Complete version history and evolution of CI/CD pipeline | Understanding what changed and why |
| [GitHub Actions Upgrade Guide](./GITHUB_ACTIONS_UPGRADE.md) | Detailed upgrade documentation from v1.0 to v3.0 | Deep dive into improvements and configurations |
| [GitHub Actions Quick Reference](./GITHUB_ACTIONS_QUICKREF.md) | One-page cheat sheet for common CI operations | Quick lookup for workflow triggers, jobs, timings |
| [CI/CD Documentation](./CI_CD.md) | Complete CI/CD pipeline reference | Understanding current pipeline structure |
| [CI Troubleshooting Guide](./CI_TROUBLESHOOTING.md) | Solutions for common CI issues | When builds fail or need debugging |
| [Local Quality Gates](./LOCAL_QUALITY_GATES.md) | Run CI checks on your local machine | Before pushing code to avoid CI failures |
| [Quality Gates Quick Reference](./QUALITY_GATES_QUICKREF.md) | Local development commands cheat sheet | Daily development reference |

### Architecture Documentation

| Document | Purpose | When to Use |
|----------|---------|-------------|
| [Architecture Overview](./ARCHITECTURE.md) | System architecture and design decisions | Understanding system structure |
| [Context Map](./CONTEXT_MAP.md) | Bounded context relationships | Understanding domain boundaries |
| [Architecture Testing Guide](./ARCHITECTURE_TESTING_GUIDE.md) | ArchUnit test patterns and rules | Writing new architecture tests |
| [Platform Shared Guide](./PLATFORM_SHARED_GUIDE.md) | Shared platform components | Using shared infrastructure |

### Development Guides

| Document | Purpose | When to Use |
|----------|---------|-------------|
| [Build System Update](./BUILD_SYSTEM_UPDATE.md) | Gradle build configuration | Understanding build setup |
| [Error Handling Analysis](./ERROR_HANDLING_ANALYSIS_AND_POLICY.md) | Error handling patterns | Implementing error handling |
| [Kafka Integration Summary](./KAFKA_INTEGRATION_SUMMARY.md) | Event-driven architecture | Working with Kafka events |
| [EDA Audit Summary](./EDA_AUDIT_SUMMARY.md) | Event-driven audit results | Event architecture review |

### Deployment Documentation

| Document | Purpose | When to Use |
|----------|---------|-------------|
| [Quick Deploy Guide](./QUICK_DEPLOY_GUIDE.md) | Step-by-step deployment instructions | Deploying to environments |
| [Port Mappings](./PORT_MAPPINGS.md) | Service port assignments | Configuring services |

### Testing Documentation

| Document | Purpose | When to Use |
|----------|---------|-------------|
| [Test Coverage - Identity Domain](./TEST_COVERAGE_IDENTITY_DOMAIN.md) | Identity domain test coverage report | Understanding test gaps |

### Production Readiness

| Document | Purpose | When to Use |
|----------|---------|-------------|
| [Production Readiness Certification Index](./PRODUCTION_READINESS_CERT_INDEX.md) | Production readiness checklist index | Pre-production verification |
| [Production Readiness Part 1](./PRODUCTION_READINESS_CERT_PART1.md) | Infrastructure & security checks | Infrastructure review |
| [Production Readiness Part 2](./PRODUCTION_READINESS_CERT_PART2.md) | Application & monitoring checks | Application review |
| [Production Readiness Part 3](./PRODUCTION_READINESS_CERT_PART3.md) | Operations & incident response | Operations review |

---

## 🚀 Getting Started

### For New Developers
1. Read [Architecture Overview](./ARCHITECTURE.md)
2. Set up [Local Quality Gates](./LOCAL_QUALITY_GATES.md)
3. Review [CI/CD Documentation](./CI_CD.md)
4. Check [GitHub Actions Quick Reference](./GITHUB_ACTIONS_QUICKREF.md)

### For CI/CD Changes
1. Read [CI Evolution Changelog](./CI_EVOLUTION_CHANGELOG.md)
2. Review [GitHub Actions Upgrade Guide](./GITHUB_ACTIONS_UPGRADE.md)
3. Test with [Local Quality Gates](./LOCAL_QUALITY_GATES.md)
4. Refer to [CI Troubleshooting Guide](./CI_TROUBLESHOOTING.md) if issues arise

### For Deployment
1. Follow [Quick Deploy Guide](./QUICK_DEPLOY_GUIDE.md)
2. Check [Port Mappings](./PORT_MAPPINGS.md)
3. Verify [Production Readiness Certification](./PRODUCTION_READINESS_CERT_INDEX.md)

---

## 📊 CI/CD Pipeline Status

**Current Version:** v3.0 (Production-Grade with Network Resilience)  
**Last Updated:** 2025-11-09

### Key Features
- ✅ Automatic retry on network failures (95% self-healing)
- ✅ Dependency cache warmup (fast cold starts)
- ✅ Parallel job execution (70% faster than sequential)
- ✅ Log gate scanning (catches unexpected errors)
- ✅ Security scanning (Trivy vulnerability detection)
- ✅ All actions pinned (reproducible builds)

### Pipeline Metrics
- Cold run: ~30-32 minutes
- Warm run: ~28-30 minutes
- Network failure recovery: Automatic (3 retries)
- Success rate: 99%+

---

## 🔧 Maintenance Schedule

### Daily
- Monitor CI pipeline runs for failures
- Review and address security scan findings

### Weekly
- Review log gate allowlist for obsolete entries
- Check for GitHub Actions deprecation warnings

### Monthly
- Review pipeline performance metrics
- Update documentation for any changes

### Quarterly
- Review and update all action versions
- Analyze failure patterns and optimize
- Update troubleshooting guide with new patterns

---

## 📝 Contributing to Documentation

### Documentation Standards
- Use Markdown format
- Include code examples where applicable
- Add "Last Updated" date
- Link to related documents
- Keep language clear and concise

### When to Update Documentation
- After CI/CD workflow changes
- When adding new features
- After troubleshooting new issues
- When deprecating old patterns

### Documentation Review Process
1. Create/update documentation file
2. Add entry to this index if new document
3. Update "Last Updated" dates
4. Cross-reference related documents
5. Submit PR with documentation changes

---

## 🔗 External Resources

### GitHub Actions
- [Official Documentation](https://docs.github.com/en/actions)
- [Best Practices](https://docs.github.com/en/actions/learn-github-actions/security-hardening-for-github-actions)
- [Workflow Syntax](https://docs.github.com/en/actions/reference/workflow-syntax-for-github-actions)

### Gradle
- [Build Action](https://github.com/gradle/actions)
- [Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
- [Build Scans](https://scans.gradle.com/)

### Tools
- [nick-fields/retry](https://github.com/nick-fields/retry)
- [Trivy Scanner](https://github.com/aquasecurity/trivy)
- [ktlint](https://pinterest.github.io/ktlint/)

---

## 📞 Support

### For CI/CD Issues
1. Check [CI Troubleshooting Guide](./CI_TROUBLESHOOTING.md)
2. Review [CI Evolution Changelog](./CI_EVOLUTION_CHANGELOG.md)
3. Search workflow run logs in GitHub Actions
4. Contact DevOps team if unresolved

### For Documentation Issues
1. Submit issue with documentation feedback
2. Suggest improvements via PR
3. Contact documentation maintainer

---

**Last Updated:** 2025-11-09  
**Maintainer:** Development Team  
**Version:** 1.0
