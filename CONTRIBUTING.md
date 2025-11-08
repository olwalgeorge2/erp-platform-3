# Contributing to ERP Platform

Thank you for your interest in contributing to the ERP Platform! This document provides guidelines and instructions for contributing to this project.

## Table of Contents
1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Workflow](#development-workflow)
4. [Coding Standards](#coding-standards)
5. [Testing Guidelines](#testing-guidelines)
6. [Documentation](#documentation)
7. [Pull Request Process](#pull-request-process)

## Code of Conduct

This project adheres to professional standards of conduct. Please be respectful, inclusive, and constructive in all interactions.

## Getting Started

### Prerequisites
- JDK 21 (Temurin recommended)
- Kotlin 2.2+
- Git
- IDE with Kotlin support (IntelliJ IDEA recommended)

### Initial Setup
```bash
# Fork and clone the repository
git clone https://github.com/YOUR-USERNAME/erp-platform-3.git
cd erp-platform-3

# Add upstream remote
git remote add upstream https://github.com/olwalgeorge2/erp-platform-3.git

# Copy environment configuration
cp .env.example .env

# Verify build
./gradlew build
```

## Development Workflow

### 1. Create a Feature Branch
```bash
# Update your main branch
git checkout main
git pull upstream main

# Create a feature branch
git checkout -b feature/your-feature-name
# or
git checkout -b fix/issue-description
```

### 2. Make Changes
- Write clean, readable code
- Follow the architectural patterns (hexagonal architecture, DDD)
- Keep changes focused and atomic
- Write meaningful commit messages

### 3. Run Local Checks
```bash
# Format code
./gradlew ktlintFormat

# Run tests
./gradlew test

# Run all checks
./gradlew check

# Build everything
./gradlew build
```

### 4. Commit Your Changes
```bash
# Stage changes
git add .

# Commit with descriptive message
git commit -m "feat: add user authentication to tenancy service"

# Or for bug fixes
git commit -m "fix: resolve null pointer in inventory service"
```

**Commit Message Format:**
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `refactor:` Code refactoring
- `test:` Test additions/changes
- `chore:` Build/tooling changes
- `perf:` Performance improvements

### 5. Push and Create Pull Request
```bash
# Push to your fork
git push origin feature/your-feature-name

# Create PR via GitHub UI
```

## Coding Standards

### Kotlin Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use ktlint for automatic formatting: `./gradlew ktlintFormat`
- Maximum line length: 140 characters
- Use meaningful variable and function names
- Prefer immutability (val over var)
- Use data classes for DTOs and value objects

### Architecture Patterns
- **Domain Layer**: Pure business logic, no framework dependencies
- **Application Layer**: Use cases, commands, queries (CQRS)
- **Infrastructure Layer**: Framework-specific implementations

```kotlin
// ✅ Good: Domain layer is pure Kotlin
class Order(
    val id: OrderId,
    val items: List<OrderItem>,
    val status: OrderStatus
) {
    fun complete(): Order = copy(status = OrderStatus.COMPLETED)
}

// ❌ Bad: Domain layer with framework dependency
@Entity // Don't use JPA annotations in domain
class Order { ... }
```

### Dependency Rules
- Domain layer: No external dependencies
- Application layer: Domain layer only
- Infrastructure layer: Domain + Application layers
- Shared modules: Domain primitives only

### Naming Conventions
- **Entities**: Nouns (Order, Customer, Product)
- **Value Objects**: Descriptive names (OrderId, EmailAddress, Money)
- **Services**: Verb + Noun (CreateOrder, CalculateTotal)
- **Repositories**: Entity + Repository (OrderRepository)
- **Commands**: Imperative (CreateOrderCommand, UpdateInventoryCommand)
- **Queries**: Question-style (GetOrderByIdQuery, ListActiveCustomersQuery)

## Testing Guidelines

### Test Pyramid
1. **Unit Tests**: Fast, isolated, test single units
2. **Integration Tests**: Test component interactions
3. **Contract Tests**: Verify API contracts
4. **Architecture Tests**: Enforce structural rules

### Writing Tests
```kotlin
// Use Kotest for assertions
class OrderServiceTest {
    @Test
    fun `should create order with valid items`() {
        // Given
        val items = listOf(OrderItem(...))
        val command = CreateOrderCommand(items)
        
        // When
        val result = orderService.createOrder(command)
        
        // Then
        result.shouldBeRight()
        result.getOrNull()?.status shouldBe OrderStatus.PENDING
    }
}
```

### Test Coverage
- Aim for 80%+ coverage on business logic
- 100% coverage on critical paths
- Use `./gradlew koverHtmlReport` to generate coverage reports

### Mock vs Real
- Use MockK for unit tests
- Use real implementations for integration tests
- Avoid mocking value objects

## Documentation

### Code Documentation
- Document public APIs with KDoc
- Explain "why" not "what" in comments
- Keep comments up-to-date with code

```kotlin
/**
 * Creates a new order for the specified customer.
 * 
 * This operation validates inventory availability and reserves items
 * before creating the order. If any item is unavailable, the entire
 * operation fails atomically.
 * 
 * @param command Contains customer ID and order items
 * @return Result with OrderId or validation errors
 */
fun createOrder(command: CreateOrderCommand): Result<OrderId>
```

### Architecture Documentation
- Update `docs/ARCHITECTURE.md` for structural changes
- Create ADRs in `docs/adr/` for significant decisions
- Update bounded context README files
- Keep diagrams current

### ADR Template
```markdown
# ADR-XXX: Title

## Status
Proposed | Accepted | Deprecated | Superseded

## Context
What is the issue we're addressing?

## Decision
What did we decide?

## Consequences
What are the implications?
```

## Pull Request Process

### Pull Request Checklist

- Code style
  - `./gradlew ktlintFormat` (fix) and `./gradlew ktlintCheck` (verify)
- Unit/integration tests
  - `./gradlew test` (module) or `./gradlew build` (root)
- Architecture tests (enforced in CI)
  - Run locally before pushing: `./gradlew :tests:arch:test`
  - CI requires both build and architecture-tests to pass; the final gate job is `build-status`.
- Documentation
  - Update ADRs if architectural decisions are affected (`docs/adr/`)
  - Update module README if public behavior changes (e.g., identity auth behavior)
- Security & tenancy
  - For identity changes, ensure anti-enumeration and timing guards remain intact
  - Confirm multi-tenant scoping and tenant headers in any new endpoints

### Pre-commit Hook Policy and Bypass
- Hooks run architecture tests before committing to catch violations early.
- Bypass only for exceptional cases (e.g., urgent hotfix workflow issues or known false positives):
  - Bash/Git Bash: `SKIP_ARCH_HOOK=1 git commit -m "hotfix: ..."`
  - PowerShell: `$env:SKIP_ARCH_HOOK='1'; git commit -m "hotfix: ..."; Remove-Item Env:SKIP_ARCH_HOOK`
- CI still enforces architecture tests; bypassing the hook does not bypass CI gates.
- After any bypass, run `./gradlew :tests:arch:test` locally and ensure a green CI build.

### Before Submitting
- [ ] Code follows style guidelines (ktlint passes)
- [ ] All tests pass locally
- [ ] New tests added for new functionality
- [ ] Documentation updated
- [ ] No merge conflicts with main
- [ ] Commit messages are clear

### PR Description
Use the provided template and include:
- **Description**: What and why
- **Related Issues**: Link to issues
- **Type of Change**: Bug fix, feature, etc.
- **Bounded Context**: Which context affected
- **Testing**: How to verify
- **Screenshots**: If UI changes

### Review Process
1. Automated CI checks must pass
2. At least one approval required
3. Address reviewer feedback
4. Squash commits if requested
5. Maintainer will merge

### CI Checks
All PRs must pass:
- ✅ Build succeeds
- ✅ All tests pass
- ✅ ktlint style check
- ✅ No new warnings
- ✅ Architecture tests pass

## Questions?

- Review existing documentation in `docs/`
- Check open/closed issues for similar questions
- Ask in pull request comments
- Contact maintainers

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.

---

Thank you for contributing to the ERP Platform! 🚀
