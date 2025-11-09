# Developer Advisory: Implementation Best Practices

**Effective Date:** 2025-11-09  
**Last Updated:** 2025-11-09  
**Version:** 1.0  
**Applies To:** All developers implementing remaining bounded contexts  
**Foundation:** Lessons learned from tenancy-identity implementation (Phase 2.1)

**Related Documentation:**
- 📋 [Implementation Roadmap](../ROADMAP.md) - Overall project timeline and Phase 1 completion
- 📐 [ADR-008: CI/CD Architecture](adr/ADR-008-cicd-network-resilience.md) - CI/CD v3.0 decisions and network resilience patterns
- 🔍 [Reviews Index](REVIEWS_INDEX.md) - Detailed review cycles for tenancy-identity (Grade A-/A)

---

## Executive Summary

This advisory consolidates **critical lessons learned** from implementing the tenancy-identity bounded context (Phase 2.1) to accelerate development of the remaining 11 contexts. It provides battle-tested patterns, common pitfalls, and senior-level engineering practices that will save weeks of iteration and prevent production issues.

**Key Achievement:** Tenancy-identity reached production quality (Grade A-/A) through 4 review cycles. This document captures those insights to enable **first-time-right** implementation for future modules.

### What's Inside
- ✅ **Proven patterns** from 85% completed tenancy-identity implementation
- ✅ **Security best practices** preventing enumeration attacks and information leakage
- ✅ **Architecture enforcement** via ArchUnit tests (CI-blocking)
- ✅ **Error handling policy** balancing security, UX, and observability
- ✅ **Event-driven integration** following ADR-007 hybrid pattern
- ✅ **Performance optimizations** learned from review cycles
- ✅ **CI/CD resilience** with 99%+ reliability (ADR-008)

### Document Structure
1. **Critical DO's and DON'Ts** - Start here for immediate impact
2. **Layer-by-Layer Implementation Guide** - Domain → Application → Infrastructure
3. **Security & Error Handling** - Production-grade patterns (OWASP-aligned)
4. **Event-Driven Integration** - ADR-007 hybrid pattern implementation
5. **Testing Strategy** - From unit to integration (test pyramid)
6. **Platform-Shared Governance** - ADR-006 compliance rules
7. **CI/CD & Quality Gates** - ADR-008 network resilience
8. **Common Pitfalls & Solutions** - Real issues from review cycles

### Success Metrics
- **Tenancy-Identity Achievement:** A- (93/100) after 4 review batches
- **Target for New Contexts:** A (95/100) on first review
- **Time Savings:** Est. 40-60 hours per context (vs. trial-and-error)
- **Production Readiness:** Security + performance + observability by default

### Cross-Reference Navigation
- **For CI/CD details:** See [ADR-008](adr/ADR-008-cicd-network-resilience.md) for complete pipeline architecture, retry strategies, and network resilience patterns
- **For project timeline:** See [ROADMAP.md](../ROADMAP.md) Phase 2 Task 3.1 for tenancy-identity implementation status
- **For architecture decisions:** See [docs/adr/](adr/) for all ADRs including ADR-006 (Platform-Shared), ADR-007 (Event-Driven)

---

## Table of Contents

1. [Critical DO's and DON'Ts](#1-critical-dos-and-donts)
2. [Layer-by-Layer Implementation Guide](#2-layer-by-layer-implementation-guide)
3. [Security & Error Handling](#3-security--error-handling)
4. [Event-Driven Integration](#4-event-driven-integration)
5. [Testing Strategy](#5-testing-strategy)
6. [Platform-Shared Governance](#6-platform-shared-governance)
7. [CI/CD & Quality Gates](#7-cicd--quality-gates)
8. [Common Pitfalls & Solutions](#8-common-pitfalls--solutions)
9. [Quick Reference](#9-quick-reference)

---

## 1. Critical DO's and DON'Ts

### 🟢 DO: Start with Domain Layer (Inside-Out)

**✅ Correct Order:**
```
1. Domain entities & aggregates (pure business logic)
2. Domain events (what happened)
3. Repository ports (interfaces in application layer)
4. Application services (use cases)
5. Infrastructure adapters (JPA, REST, Kafka)
```

**Why:** Domain-driven design keeps business logic isolated from frameworks.

**Example from tenancy-identity:**
```kotlin
// 1. Domain entity (identity-domain/)
data class User(
    val id: UserId,
    val tenantId: TenantId,
    val username: String,
    private val passwordHash: String,
    val email: String,
    val status: UserStatus,
) {
    fun authenticate(password: String, crypto: PasswordCrypto): Result<User> {
        // Pure business logic - no framework dependencies
    }
}

// 2. Domain event (identity-domain/events/)
data class UserCreatedEvent(
    override val eventId: String,
    override val occurredAt: Instant,
    override val aggregateId: String,
    val tenantId: String,
    val username: String,
    val email: String,
) : DomainEvent

// 3. Repository port (identity-application/port/output/)
interface UserRepository {
    fun findById(tenantId: TenantId, userId: UserId): Result<User>
    fun save(user: User): Result<User>
}

// 4. Application service (identity-application/)
@ApplicationScoped
class IdentityCommandService(
    private val userRepository: UserRepository,
    private val eventPublisher: EventPublisherPort,
) {
    fun createUser(command: CreateUserCommand): Result<User>
}

// 5. Infrastructure adapter (identity-infrastructure/)
@ApplicationScoped
class JpaUserRepository : UserRepository {
    override fun save(user: User): Result<User>
}
```

### 🔴 DON'T: Start with REST API or Database Schema

**❌ Wrong Approach:**
- Writing REST endpoints first
- Designing database tables before domain models
- Adding framework annotations to domain entities

**Why It Fails:** Business logic becomes coupled to frameworks, making testing and evolution difficult.

---

### 🟢 DO: Use Result<T> Monad for Error Handling

**✅ All repository methods return Result<T>:**
```kotlin
// Port definition (application layer)
interface TenantRepository {
    fun findById(tenantId: TenantId): Result<Tenant>
    fun save(tenant: Tenant): Result<Tenant>
    fun existsBySlug(slug: String): Result<Boolean>
}

// Adapter implementation (infrastructure layer)
override fun save(tenant: Tenant): Result<Tenant> = try {
    val entity = mapper.toEntity(tenant)
    entityManager.persist(entity)
    Result.success(mapper.toDomain(entity))
} catch (e: PersistenceException) {
    when {
        e.message?.contains("slug") == true ->
            Result.failure("TENANT_SLUG_EXISTS", "Tenant slug already exists")
        else -> {
            Log.errorf(e, "Persistence error")
            Result.failure("DATABASE_ERROR", "Database operation failed")
        }
    }
}

// Usage in application service
fun createTenant(command: CreateTenantCommand): Result<Tenant> =
    validateSlug(command.slug)
        .flatMap { checkSlugAvailability(command.slug) }
        .flatMap { createTenantAggregate(command) }
        .flatMap { tenant -> tenantRepository.save(tenant) }
        .onSuccess { tenant -> publishEvent(TenantProvisionedEvent.from(tenant)) }
```

**Benefits:**
- ✅ Type-safe error handling (no exceptions in business logic)
- ✅ Composable with `flatMap`, `map`, `onSuccess`, `onFailure`
- ✅ Consistent error structure across codebase
- ✅ Forces explicit error handling at call sites

### 🔴 DON'T: Return Nullable Types or Throw Exceptions

**❌ Wrong Patterns:**
```kotlin
// DON'T: Nullable return types
interface UserRepository {
    fun findById(id: UserId): User?  // ❌ Loses error context
}

// DON'T: Throwing exceptions from domain/application layer
fun createUser(command: CreateUserCommand): User {
    if (userRepository.existsByUsername(command.username)) {
        throw UsernameAlreadyExistsException()  // ❌ Not type-safe
    }
}

// DON'T: Catching generic exceptions
try {
    userRepository.save(user)
} catch (e: Exception) {  // ❌ Too broad
    return null
}
```

**Why It Fails:**
- Nullable types lose error information (why did it fail?)
- Exceptions break control flow and are hard to compose
- Generic exception catching hides root causes

---

### 🟢 DO: Implement Transactional Outbox Pattern for Events

**✅ Required for all contexts publishing events:**

```kotlin
// 1. Outbox entity (infrastructure/jpa/)
@Entity
@Table(name = "event_outbox")
data class OutboxEventEntity(
    @Id val id: UUID,
    val eventType: String,
    val aggregateId: String,
    val tenantId: String?,
    val payload: String,  // JSON
    val createdAt: Instant,
    var publishedAt: Instant? = null,
    var failureCount: Int = 0,
    var lastError: String? = null,
)

// 2. Outbox publisher (infrastructure/adapter/)
@ApplicationScoped
class OutboxEventPublisher(
    private val entityManager: EntityManager,
) : EventPublisherPort {
    @Transactional
    override fun publish(event: DomainEvent) {
        val outboxEvent = OutboxEventEntity(
            id = UUID.randomUUID(),
            eventType = event::class.simpleName!!,
            aggregateId = event.aggregateId,
            tenantId = (event as? TenantScoped)?.tenantId,
            payload = Json.encodeToString(event),
            createdAt = Instant.now(),
        )
        entityManager.persist(outboxEvent)
    }
}

// 3. Outbox scheduler (infrastructure/scheduler/)
@ApplicationScoped
class OutboxEventScheduler(
    private val outboxRepository: OutboxRepository,
    private val kafkaProducer: KafkaProducer,
) {
    @Scheduled(every = "5s", concurrentExecution = SKIP)
    @Transactional
    fun publishPendingEvents() {
        val pending = outboxRepository.findUnpublished(limit = 100)
        
        pending.forEach { event ->
            try {
                kafkaProducer.send(
                    topic = "${contextName}.domain.events.v1",
                    key = event.aggregateId,
                    value = event.payload,
                    headers = mapOf(
                        "event-type" to event.eventType,
                        "tenant-id" to event.tenantId,
                        "trace-id" to MDC.get("traceId"),
                    ),
                )
                event.publishedAt = Instant.now()
                outboxRepository.save(event)
                
                metricsCollector.increment("${contextName}.outbox.events.published")
            } catch (e: Exception) {
                event.failureCount++
                event.lastError = e.message
                outboxRepository.save(event)
                
                Log.errorf(e, "Failed to publish event %s", event.id)
                metricsCollector.increment("${contextName}.outbox.events.failed")
            }
        }
    }
}
```

**Why Mandatory:**
- ✅ **Atomic:** Event persisted in same transaction as domain changes
- ✅ **Reliable:** No lost events if Kafka is down
- ✅ **Traceable:** Outbox table provides audit trail
- ✅ **Retryable:** Failed events automatically retried

### 🔴 DON'T: Publish Events Directly to Kafka in Application Layer

**❌ Wrong Pattern:**
```kotlin
@ApplicationScoped
class UserCommandHandler(
    private val userRepository: UserRepository,
    private val kafkaProducer: KafkaProducer,  // ❌ Direct Kafka dependency
) {
    @Transactional
    fun createUser(command: CreateUserCommand): Result<User> {
        return userRepository.save(user)
            .onSuccess { user ->
                // ❌ Event lost if Kafka fails after commit
                kafkaProducer.send("users.events", UserCreatedEvent.from(user))
            }
    }
}
```

**Why It Fails:**
- ❌ Events lost if Kafka is unavailable
- ❌ Application layer depends on infrastructure (Kafka)
- ❌ No retry mechanism for failed publishes
- ❌ Distributed transaction issues

---

### 🟢 DO: Implement Password/Credential Security Correctly

**✅ Use Argon2id with PBKDF2 fallback (from tenancy-identity):**

```kotlin
interface PasswordCrypto {
    fun hash(password: String): String
    fun verify(password: String, hash: String): Boolean
}

@ApplicationScoped
class Argon2PasswordCrypto : PasswordCrypto {
    private val argon2 = Argon2Factory.create(
        Argon2Factory.Argon2Types.ARGON2id,
        32,  // Salt length
        64,  // Hash length
    )
    
    override fun hash(password: String): String {
        return argon2.hash(
            3,      // Iterations
            19456,  // Memory (19MB)
            1,      // Parallelism
            password.toCharArray(),
        )
    }
    
    override fun verify(password: String, hash: String): Boolean {
        return when {
            hash.startsWith("\$argon2id$") -> 
                argon2.verify(hash, password.toCharArray())
            hash.startsWith("pbkdf2:") -> 
                pbkdf2Verify(password, hash)  // Legacy fallback
            else -> false
        }
    }
}
```

**Security requirements:**
- ✅ Argon2id (winner of Password Hashing Competition)
- ✅ Minimum 3 iterations, 19MB memory
- ✅ PBKDF2 fallback for legacy credentials (120k iterations)
- ✅ Test coverage for all hash algorithms
- ✅ Anti-enumeration timing guards (see Security section)

### 🔴 DON'T: Use MD5, SHA1, or bcrypt for New Passwords

**❌ Wrong Algorithms:**
```kotlin
// ❌ MD5 - Cryptographically broken
val hash = MessageDigest.getInstance("MD5").digest(password.toByteArray())

// ❌ SHA-256 without salt - Rainbow table attacks
val hash = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())

// ❌ bcrypt - Outdated (GPU-friendly)
val hash = BCrypt.hashpw(password, BCrypt.gensalt())
```

**Why It Fails:**
- MD5/SHA1: Broken cryptographic algorithms
- SHA-256 without salt: Vulnerable to rainbow tables
- bcrypt: GPU-friendly (Argon2id is GPU-resistant)

---

### 🟢 DO: Implement Anti-Enumeration Protection

**✅ Critical for authentication endpoints:**

```kotlin
fun authenticateUser(command: AuthenticateCommand): Result<User> {
    val user = userRepository.findByUsername(
        command.tenantId,
        command.username,
    ).getOrNull()
    
    return when {
        user == null -> {
            // Anti-enumeration: Constant-time response for non-existent users
            try {
                Thread.sleep(100)  // Match successful auth timing
            } catch (_: InterruptedException) {}
            
            Result.failure(
                code = "AUTHENTICATION_FAILED",  // Generic message
                message = "Authentication failed",
            )
        }
        
        !crypto.verify(command.password, user.passwordHash) -> {
            Result.failure(
                code = "AUTHENTICATION_FAILED",  // Same error code
                message = "Authentication failed",
            )
        }
        
        else -> Result.success(user)
    }
}
```

**Why Critical:**
- ✅ **Timing attack prevention:** Similar response time for invalid username vs invalid password
- ✅ **Enumeration prevention:** Can't determine if username exists
- ✅ **Generic error:** Same error code/message for all auth failures

**Apply to these operations:**
- Authentication/login endpoints
- Password reset requests
- Username/email availability checks (for unauthenticated users)

### 🔴 DON'T: Return Different Errors for Username vs Password

**❌ Wrong Pattern (enables enumeration):**
```kotlin
fun authenticateUser(command: AuthenticateCommand): Result<User> {
    val user = userRepository.findByUsername(command.username).getOrNull()
        ?: return Result.failure(
            code = "USER_NOT_FOUND",  // ❌ Reveals username doesn't exist
            message = "User not found",
        )
    
    if (!crypto.verify(command.password, user.passwordHash)) {
        return Result.failure(
            code = "INVALID_PASSWORD",  // ❌ Reveals username exists
            message = "Invalid password",
        )
    }
    
    return Result.success(user)
}
```

**Why It Fails:** Attackers can enumerate valid usernames by observing different error codes.

---

### 🟢 DO: Enforce Code Style with ktlint (Auto-Format Before Commit)

**✅ Mandatory workflow for all code changes:**

```powershell
# 1. Auto-format code (fixes 95% of style violations)
./gradlew ktlintFormat

# 2. Verify formatting
./gradlew ktlintCheck

# 3. Commit formatted code
git add .
git commit -m "feat: implement user service"
```

**ktlint Configuration (applied to all subprojects):**
```kotlin
// Root build.gradle.kts
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.3.1")
    verbose.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)  // ✅ CI fails on style violations
    
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}
```

**EditorConfig Rules (.editorconfig):**
```ini
# Kotlin code style
[*.{kt,kts}]
indent_size = 4
max_line_length = 120
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

# ktlint-specific rules
ktlint_standard_no-wildcard-imports = enabled
ktlint_standard_filename = disabled
ktlint_function_naming_ignore_when_annotated_with = Composable

# YAML consistency
[*.{yml,yaml}]
indent_size = 2

# JSON consistency
[*.json]
indent_size = 2
```

**Why Critical:**
- ✅ **Consistent formatting:** No debates about style in code reviews
- ✅ **Clean diffs:** Format before commit = no noise in PRs
- ✅ **CI enforcement:** Build fails if style violations detected
- ✅ **Auto-fix available:** ktlintFormat fixes 95% automatically

**Common Style Rules:**
| Rule | Requirement | Example |
|------|-------------|---------|
| Max line length | 120 characters | Enforced by ktlint |
| Indentation | 4 spaces (Kotlin), 2 spaces (YAML/JSON) | Auto-formatted |
| Wildcard imports | Disallowed | `import kotlin.collections.*` ❌ |
| Trailing commas | Allowed (recommended) | `listOf("a", "b",)` ✅ |
| Final newline | Required | All files end with `\n` |
| Trailing whitespace | Disallowed | Auto-removed by ktlintFormat |

**IDE Integration (IntelliJ IDEA):**
```
Settings → Editor → Code Style → Kotlin
- Set from: EditorConfig
- ✅ Enable EditorConfig support
- ✅ Apply code style to: Kotlin
```

**Pre-commit Hook (optional but recommended):**
```bash
#!/bin/bash
# .git/hooks/pre-commit

echo "Running ktlintFormat..."
./gradlew ktlintFormat --quiet

if [ $? -eq 0 ]; then
    echo "✅ Code formatted successfully"
    git add -u  # Stage formatting changes
else
    echo "❌ ktlintFormat failed"
    exit 1
fi
```

### 🔴 DON'T: Commit Code Without Running ktlintFormat

**❌ Common Mistakes:**
```kotlin
// ❌ Wildcard imports (disallowed)
import kotlin.collections.*
import com.example.erp.domain.*

// ❌ Line too long (> 120 chars)
val result = userRepository.findByUsernameAndTenantId(username, tenantId).map { user -> user.toDto() }.getOrElse { null }

// ❌ Inconsistent indentation
fun createUser(
 command: CreateUserCommand  // ❌ 1 space instead of 4
): Result<User> {
        return userRepository.save(user)  // ❌ 8 spaces instead of 4
}

// ❌ Trailing whitespace
val username = "admin"   
```

**Why It Fails:**
- CI pipeline fails ktlintCheck gate (wastes build time)
- Code review noise: "fix formatting" comments
- Git diffs polluted with formatting changes

**✅ Correct (after ktlintFormat):**
```kotlin
// ✅ Explicit imports
import kotlin.collections.List
import com.example.erp.domain.User
import com.example.erp.domain.UserId

// ✅ Proper line breaking
val result = userRepository
    .findByUsernameAndTenantId(username, tenantId)
    .map { user -> user.toDto() }
    .getOrElse { null }

// ✅ Consistent indentation (4 spaces)
fun createUser(
    command: CreateUserCommand
): Result<User> {
    return userRepository.save(user)
}

// ✅ No trailing whitespace
val username = "admin"
```

---

### 🟢 DO: Add Database Indexes on Query Patterns

**✅ Index all foreign keys and frequently queried fields:**

```sql
-- Migration: V003__add_user_indexes.sql

-- Composite index for tenant-scoped user lookups (most common query)
CREATE INDEX idx_users_tenant_username 
    ON users(tenant_id, username);

CREATE INDEX idx_users_tenant_email 
    ON users(tenant_id, email);

-- Tenant-scoped queries
CREATE INDEX idx_users_tenant_status 
    ON users(tenant_id, status);

-- Foreign key indexes
CREATE INDEX idx_users_tenant_id 
    ON users(tenant_id);

CREATE INDEX idx_role_assignments_user_id 
    ON role_assignments(user_id);

CREATE INDEX idx_role_assignments_role_id 
    ON role_assignments(role_id);

-- Outbox processing
CREATE INDEX idx_outbox_unpublished 
    ON event_outbox(published_at) 
    WHERE published_at IS NULL;

CREATE INDEX idx_outbox_created_at 
    ON event_outbox(created_at DESC);
```

**Index strategy:**
- ✅ Composite indexes for multi-column WHERE clauses
- ✅ Foreign key columns (JOIN performance)
- ✅ Unique constraints become indexes automatically
- ✅ Partial indexes for filtered queries (WHERE clause)

### 🔴 DON'T: Skip Indexes or Over-Index

**❌ Common mistakes:**
```sql
-- ❌ No index on foreign key
ALTER TABLE orders ADD COLUMN customer_id UUID;

-- ❌ Over-indexing (every column)
CREATE INDEX idx_users_id ON users(id);  -- Already PK
CREATE INDEX idx_users_created ON users(created_at);  -- Rarely queried
```

**Why It Fails:**
- Missing FK indexes cause slow JOINs (O(n) vs O(log n))
- Too many indexes slow down writes (INSERT/UPDATE)
- Index maintenance overhead

---

### 🟢 DO: Implement Structured Logging with Correlation IDs

**✅ MDC-based correlation tracking:**

```kotlin
// Request filter (infrastructure/adapter/input/rest/)
@Provider
class RequestLoggingFilter : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        val traceId = requestContext.getHeaderString("X-Trace-Id")
            ?: UUID.randomUUID().toString()
        
        MDC.put("traceId", traceId)
        MDC.put("tenantId", extractTenantId(requestContext))
        MDC.put("userId", extractUserId(requestContext))
        MDC.put("path", requestContext.uriInfo.path)
        MDC.put("method", requestContext.method)
    }
}

// Application service logging
@ApplicationScoped
class IdentityCommandService {
    fun createUser(command: CreateUserCommand): Result<User> {
        val startTime = System.currentTimeMillis()
        
        Log.infof(
            "[%s] createUser - tenant=%s, username=%s",
            MDC.get("traceId"),
            command.tenantId,
            command.username,
        )
        
        return execute(command)
            .also { result ->
                val duration = System.currentTimeMillis() - startTime
                when (result) {
                    is Result.Success -> Log.infof(
                        "[%s] ✓ createUser completed in %d ms - userId=%s",
                        MDC.get("traceId"),
                        duration,
                        result.value.id,
                    )
                    is Result.Failure -> Log.warnf(
                        "[%s] ✗ createUser failed in %d ms - error=%s",
                        MDC.get("traceId"),
                        duration,
                        result.error.code,
                    )
                }
            }
    }
}
```

**Required fields in MDC:**
- `traceId` - Request correlation ID
- `tenantId` - Multi-tenant context
- `userId` - User context (if authenticated)
- `operation` - Business operation name

### 🔴 DON'T: Use Plain println or System.out

**❌ Wrong logging:**
```kotlin
fun createUser(command: CreateUserCommand): Result<User> {
    println("Creating user: ${command.username}")  // ❌ No structure
    System.out.println("Tenant: ${command.tenantId}")  // ❌ No correlation
}
```

**Why It Fails:**
- No correlation across distributed requests
- Can't filter/search logs effectively
- No log levels (info vs warn vs error)
- Performance issues in production

---

### 🟢 DO: Add Metrics for Business Operations

**✅ Micrometer metrics on application services:**

```kotlin
@ApplicationScoped
class IdentityCommandService {
    @Counted(
        value = "identity.user.created",
        extraTags = ["context", "identity"],
    )
    @Timed(
        value = "identity.user.create.duration",
        percentiles = [0.5, 0.95, 0.99],
    )
    fun createUser(command: CreateUserCommand): Result<User> {
        return execute(command)
            .also { result ->
                when (result) {
                    is Result.Success -> 
                        metricsCollector.increment("identity.user.created.success")
                    is Result.Failure -> 
                        metricsCollector.increment("identity.user.created.failure",
                            "error", result.error.code)
                }
            }
    }
}
```

**Required metrics:**
- **Counters:** Operation success/failure counts
- **Timers:** Operation duration (p50, p95, p99)
- **Gauges:** Active resources (connections, cache size)

**Prometheus exposition:**
```yaml
# application.yml
quarkus:
  micrometer:
    export:
      prometheus:
        enabled: true
        path: /q/metrics
```

### 🔴 DON'T: Skip Observability or Add Metrics Later

**❌ Wrong approach:**
```kotlin
// No metrics, no timing, no structured logging
fun createUser(command: CreateUserCommand): Result<User> {
    return userRepository.save(User.create(command))
}
```

**Why It Fails:**
- Can't diagnose performance issues in production
- No visibility into error rates
- SLO/SLA tracking impossible

---

## 2. Layer-by-Layer Implementation Guide

### 2.1 Domain Layer Checklist

**Start Here (Pure Business Logic):**

```
☐ Define aggregates and entities
  - No framework annotations (@Entity, @Table, etc.)
  - Value objects for type safety (UserId, Email, etc.)
  - Business validation in entity methods
  
☐ Create domain events
  - Immutable data classes
  - Implement DomainEvent interface
  - Past tense naming (UserCreatedEvent, not CreateUserEvent)
  
☐ Define repository ports (interfaces only)
  - Located in application layer
  - Return Result<T>, never null
  - Method names describe business intent
  
☐ Implement domain services (if needed)
  - Cross-aggregate business logic
  - No framework dependencies
```

**Example Domain Entity:**
```kotlin
// identity-domain/src/main/kotlin/com/erp/identity/domain/model/User.kt

data class User private constructor(
    val id: UserId,
    val tenantId: TenantId,
    val username: String,
    private val passwordHash: String,
    val email: String,
    val status: UserStatus,
    val roles: Set<RoleId> = emptySet(),
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun create(
            id: UserId,
            tenantId: TenantId,
            username: String,
            passwordHash: String,
            email: String,
        ): User {
            require(username.isNotBlank()) { "Username cannot be blank" }
            require(email.matches(EMAIL_REGEX)) { "Invalid email format" }
            
            return User(
                id = id,
                tenantId = tenantId,
                username = username,
                passwordHash = passwordHash,
                email = email,
                status = UserStatus.ACTIVE,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        }
    }
    
    fun authenticate(password: String, crypto: PasswordCrypto): Result<User> {
        return when {
            status != UserStatus.ACTIVE -> 
                Result.failure("ACCOUNT_LOCKED", "Account is not active")
            !crypto.verify(password, passwordHash) -> 
                Result.failure("AUTHENTICATION_FAILED", "Authentication failed")
            else -> Result.success(this)
        }
    }
    
    fun assignRole(roleId: RoleId): User {
        return copy(
            roles = roles + roleId,
            updatedAt = Instant.now(),
        )
    }
}
```

### 2.2 Application Layer Checklist

**Command/Query Handlers + Use Cases:**

```
☐ Define commands and queries
  - Commands: CreateUserCommand, UpdatePasswordCommand
  - Queries: FindUserByIdQuery, ListTenantUsersQuery
  
☐ Implement command handlers
  - Orchestrate use case flow
  - Use repository ports (never implementations)
  - Publish events via EventPublisherPort
  - @Transactional on write operations
  
☐ Add request/response DTOs
  - Separate from domain models
  - Bean validation annotations (@NotBlank, @Email, etc.)
  - Map between DTOs and domain models
  
☐ Define all ports (interfaces)
  - Output ports: Repositories, EventPublisher
  - Input ports: Command/Query handlers (if using port/adapter style)
```

**Example Command Handler:**
```kotlin
// identity-application/src/main/kotlin/com/erp/identity/application/UserCommandHandler.kt

@ApplicationScoped
class UserCommandHandler @Inject constructor(
    private val userRepository: UserRepository,
    private val tenantRepository: TenantRepository,
    private val passwordCrypto: PasswordCrypto,
    private val eventPublisher: EventPublisherPort,
) {
    @Counted("identity.command.createUser")
    @Timed("identity.command.createUser.duration")
    @Transactional
    fun handle(command: CreateUserCommand): Result<User> {
        Log.infof("[%s] createUser - tenant=%s, username=%s", 
            MDC.get("traceId"), command.tenantId, command.username)
        
        val startTime = System.currentTimeMillis()
        
        return tenantRepository.findById(command.tenantId)
            .flatMap { validatePasswordPolicy(command.password) }
            .flatMap { checkUsernameAvailability(command.tenantId, command.username) }
            .flatMap { checkEmailAvailability(command.tenantId, command.email) }
            .flatMap { createAndSaveUser(command) }
            .onSuccess { user ->
                eventPublisher.publish(UserCreatedEvent.from(user))
                val duration = System.currentTimeMillis() - startTime
                Log.infof("[%s] ✓ createUser completed in %d ms", 
                    MDC.get("traceId"), duration)
            }
            .onFailure { error ->
                val duration = System.currentTimeMillis() - startTime
                Log.warnf("[%s] ✗ createUser failed in %d ms - %s", 
                    MDC.get("traceId"), duration, error.code)
            }
    }
    
    private fun createAndSaveUser(command: CreateUserCommand): Result<User> {
        val passwordHash = passwordCrypto.hash(command.password)
        val user = User.create(
            id = UserId.generate(),
            tenantId = command.tenantId,
            username = command.username,
            passwordHash = passwordHash,
            email = command.email,
        )
        return userRepository.save(user)
    }
}
```

### 2.3 Infrastructure Layer Checklist

**Adapters + Technical Implementation:**

```
☐ Implement JPA repository adapters
  - @ApplicationScoped (not @Repository)
  - Implement port interfaces from application layer
  - Convert between JPA entities and domain models
  - Handle constraint violations → Result.failure
  
☐ Create JPA entities (separate from domain)
  - @Entity, @Table annotations
  - Located in infrastructure/jpa/entity/
  - Simple data holders, no business logic
  
☐ Implement REST resources
  - JAX-RS or Quarkus REST
  - Use ResultMapper for Result<T> → HTTP response
  - Request/response DTOs from application layer
  
☐ Implement event publisher adapter
  - Transactional outbox pattern (mandatory)
  - Kafka producer with headers (tenant-id, trace-id, event-type)
  - Scheduled outbox processor (5-second interval)
  
☐ Add Flyway migrations
  - V001__create_tables.sql
  - V002__add_constraints.sql
  - V003__add_indexes.sql
```

**Example JPA Repository:**
```kotlin
// identity-infrastructure/adapter/output/jpa/JpaUserRepository.kt

@ApplicationScoped
class JpaUserRepository @Inject constructor(
    private val entityManager: EntityManager,
    private val mapper: UserEntityMapper,
) : UserRepository {
    
    override fun findById(tenantId: TenantId, userId: UserId): Result<User> = try {
        val entity = entityManager
            .createQuery("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId AND u.id = :userId", 
                UserEntity::class.java)
            .setParameter("tenantId", tenantId.value)
            .setParameter("userId", userId.value)
            .singleResult
        
        Result.success(mapper.toDomain(entity))
    } catch (e: NoResultException) {
        Result.failure("USER_NOT_FOUND", "User not found")
    } catch (e: Exception) {
        Log.errorf(e, "Error finding user")
        Result.failure("DATABASE_ERROR", "Database error occurred")
    }
    
    @Transactional
    override fun save(user: User): Result<User> = try {
        val entity = mapper.toEntity(user)
        entityManager.persist(entity)
        entityManager.flush()
        
        Result.success(mapper.toDomain(entity))
    } catch (e: PersistenceException) {
        when {
            e.message?.contains("username") == true ->
                Result.failure("USERNAME_IN_USE", "Username already exists")
            e.message?.contains("email") == true ->
                Result.failure("EMAIL_IN_USE", "Email already exists")
            else -> {
                Log.errorf(e, "Persistence error")
                Result.failure("DATABASE_ERROR", e.message ?: "Database error")
            }
        }
    }
}
```

---

## 3. Security & Error Handling

### 3.1 Error Handling Policy (From ERROR_HANDLING_ANALYSIS_AND_POLICY.md)

**Priority-Based Implementation Roadmap:**

| Priority | Area | Impact | Effort | Status |
|----------|------|--------|--------|--------|
| **P0** | Sanitize error messages for production | HIGH | MEDIUM | ⚠️ Required for all contexts |
| **P0** | Rate limiting on auth endpoints | HIGH | LOW | ⚠️ Required for identity-related |
| **P0** | Generic responses for enumeration-sensitive ops | HIGH | LOW | ⚠️ Required for auth |
| **P1** | Error classification system (severity levels) | MEDIUM | MEDIUM | 📋 Planned |
| **P1** | Centralized error response factory | MEDIUM | LOW | 📋 Planned |
| **P2** | Retry policies for transient failures | MEDIUM | HIGH | 📋 Future |
| **P2** | Circuit breakers for external dependencies | MEDIUM | HIGH | 📋 Future |

**Three-Layer Error Strategy:**

```
┌──────────────────────────────────────────────────────┐
│  Layer 1: External API (REST)                        │
│  Production: Generic messages only                   │
│  • "Service temporarily unavailable"                 │
│  • "The provided data is invalid"                    │
│  • "We couldn't find that resource"                  │
│  Dev/Test: Full error details for debugging         │
└──────────────┬───────────────────────────────────────┘
               │
┌──────────────▼───────────────────────────────────────┐
│  Layer 2: Internal Logging (Always Full Details)    │
│  • Complete error messages                           │
│  • Stack traces (for infrastructure errors)          │
│  • Correlation IDs (traceId, tenantId, userId)      │
│  • Request/response context                          │
│  • Timing information                                │
└──────────────┬───────────────────────────────────────┘
               │
┌──────────────▼───────────────────────────────────────┐
│  Layer 3: Monitoring & Alerts                        │
│  • Error rate metrics (by code, by endpoint)         │
│  • Severity classification                           │
│  • Automated incident creation (for CRITICAL)        │
│  • SLO burn rate tracking                            │
└──────────────────────────────────────────────────────┘
```

**Error Classification (OWASP-Aligned):**

| Severity | Examples | HTTP Status | Alert | Production Message | Internal Log |
|----------|----------|-------------|-------|--------------------|--------------|
| **CRITICAL** | Database down, Kafka unavailable, OOM | 503 | Yes (PagerDuty) | "Service temporarily unavailable. Please try again later." | Full exception + stack trace |
| **HIGH** | Auth failures, duplicate keys | 400-409 | No | Context-appropriate message | Full details + user context |
| **MEDIUM** | Validation errors, business rule violations | 422 | No | Field-level feedback | Validation details |
| **LOW** | Not found errors, expected failures | 404 | No | "We couldn't find that resource." | Resource identifier only |

**Security Considerations (from ERROR_HANDLING_ANALYSIS_AND_POLICY.md):**

| Risk Level | Attack Vector | Current State (Tenancy-Identity) | Required Mitigation |
|------------|---------------|----------------------------------|---------------------|
| 🔴 **CRITICAL** | Timing attacks on authentication | ⚠️ Mitigated with 100ms delay | Apply to all auth endpoints |
| 🔴 **HIGH** | Username/email enumeration | ⚠️ Mitigated with generic errors | Apply to all registration/login |
| 🟡 **MEDIUM** | Account status disclosure | ⚠️ Mitigated with error sanitization | Apply to all user operations |
| 🟡 **MEDIUM** | Tenant existence probing | ⚠️ Mitigated with generic 404s | Apply to all tenant operations |
| 🟢 **LOW** | Resource existence via timing | ✅ Acceptable (admin operations) | No action needed |

**Implementation (ResultMapper):**
```kotlin
@ApplicationScoped
class ResultMapper {
    fun <T> toResponse(result: Result<T>): Response {
        return when (result) {
            is Result.Success -> Response
                .ok(result.value)
                .build()
            
            is Result.Failure -> {
                val sanitized = ErrorSanitizer.sanitize(result.error)
                val status = mapStatus(sanitized.code, result.validationErrors.isNotEmpty())
                
                Response.status(status)
                    .entity(ErrorResponse(
                        code = sanitized.code,
                        message = sanitized.message,  // Sanitized for production
                        details = emptyMap(),  // Never expose internal details
                        validationErrors = result.validationErrors,
                        traceId = MDC.get("traceId"),
                        timestamp = Instant.now(),
                    ))
                    .build()
            }
        }
    }
}

object ErrorSanitizer {
    fun sanitize(error: DomainError): DomainError {
        val isProduction = Config.getOptionalValue("quarkus.profile", String::class.java)
            .orElse("prod") == "prod"
        
        return if (isProduction) {
            error.copy(
                message = sanitizeMessage(error.code),
                details = emptyMap(),  // Strip all details
                cause = null,  // Strip stack trace
            )
        } else {
            error  // Full details in dev/test
        }
    }
    
    private fun sanitizeMessage(code: String): String = when {
        code.contains("NOT_FOUND") -> "We couldn't find that resource."
        code.contains("AUTHENTICATION") -> "Authentication failed."
        code.contains("FORBIDDEN") -> "You don't have permission to perform this action."
        code.contains("CONFLICT") -> "This operation conflicts with existing data."
        code.contains("VALIDATION") -> "The provided data is invalid."
        else -> "An error occurred. Please try again."
    }
}
```

### 3.2 Security Best Practices (OWASP-Aligned)

**Critical Security Patterns:**

#### A. Anti-Enumeration (Username/Email Checks)

```kotlin
// ❌ WRONG: Reveals if username exists
@POST
@Path("/check-username")
fun checkUsername(request: CheckUsernameRequest): Response {
    val exists = userRepository.existsByUsername(request.username)
    return Response.ok(mapOf("available" to !exists)).build()
}

// ✅ CORRECT: Rate-limited, logged, time-delayed
@POST
@Path("/check-username")
@RateLimited(maxRequests = 5, windowSeconds = 60)
fun checkUsername(request: CheckUsernameRequest): Response {
    Log.warnf("Username availability check from IP %s - username=%s",
        requestContext.getClientIp(), request.username)
    
    Thread.sleep(100)  // Constant-time response
    
    val exists = userRepository.existsByUsername(request.username)
    return Response.ok(mapOf("available" to !exists)).build()
}
```

#### B. Password Policy Enforcement

```kotlin
data class PasswordPolicy(
    val minLength: Int = 12,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireDigit: Boolean = true,
    val requireSpecialChar: Boolean = true,
    val forbidCommonPasswords: Boolean = true,
) {
    fun validate(password: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        if (password.length < minLength) {
            errors.add(ValidationError(
                field = "password",
                code = "TOO_SHORT",
                message = "Password must be at least $minLength characters",
            ))
        }
        
        if (requireUppercase && !password.any { it.isUpperCase() }) {
            errors.add(ValidationError(
                field = "password",
                code = "MISSING_UPPERCASE",
                message = "Password must contain an uppercase letter",
            ))
        }
        
        // ... additional checks
        
        if (forbidCommonPasswords && isCommonPassword(password)) {
            errors.add(ValidationError(
                field = "password",
                code = "COMMON_PASSWORD",
                message = "Password is too common, please choose a stronger one",
            ))
        }
        
        return errors
    }
    
    private fun isCommonPassword(password: String): Boolean {
        val common = setOf("password", "123456", "qwerty", /* ... top 10k */)
        return password.lowercase() in common
    }
}
```

#### C. Rate Limiting

```kotlin
@ApplicationScoped
class RateLimiter {
    private val buckets = ConcurrentHashMap<String, TokenBucket>()
    
    fun checkAndConsume(key: String, maxRequests: Int, windowSeconds: Long): Boolean {
        val bucket = buckets.computeIfAbsent(key) {
            TokenBucket(maxRequests, windowSeconds)
        }
        return bucket.tryConsume()
    }
}

@Provider
class RateLimitingFilter : ContainerRequestFilter {
    @Inject
    lateinit var rateLimiter: RateLimiter
    
    override fun filter(requestContext: ContainerRequestContext) {
        val clientIp = requestContext.getClientIp()
        val path = requestContext.uriInfo.path
        
        if (path.contains("/auth/") || path.contains("/check-")) {
            val allowed = rateLimiter.checkAndConsume(
                key = "$clientIp:$path",
                maxRequests = 10,
                windowSeconds = 60,
            )
            
            if (!allowed) {
                throw TooManyRequestsException("Rate limit exceeded")
            }
        }
    }
}
```

---

## 4. Event-Driven Integration (ADR-007 Hybrid Pattern)

### 4.1 EDA Pattern Selection Framework

**Decision Criteria (Use This First):**

```
Step 1: Does this context need to QUERY data from other contexts?
├─ YES → Not Pure EDA (continue to Step 2)
└─ NO → Could be Pure EDA (consumer-only)

Step 2: Does this context need to COMMAND/WRITE to other contexts?
├─ YES → Hybrid Pattern (Events for writes + REST for reads)
└─ NO → REST-Only or Pure EDA Consumer

Step 3: What is the integration volume?
├─ HIGH (>100 ops/min) → Prefer Events (async, scalable)
├─ MEDIUM (10-100 ops/min) → Hybrid (optimal balance)
└─ LOW (<10 ops/min) → REST-Only acceptable

Step 4: What is the consistency requirement?
├─ IMMEDIATE (real-time) → REST for reads (strong consistency)
├─ EVENTUAL (seconds/minutes) → Events for writes (loose coupling)
└─ MIXED → Hybrid Pattern (most common)
```

**Recommended Pattern by Context (from EDA_AUDIT_SUMMARY.md):**

| Context | Pattern | Events Published | REST APIs Exposed | Events Consumed | Justification |
|---------|---------|------------------|-------------------|-----------------|---------------|
| **Tenancy-Identity** ✅ | **Hybrid** | User*, Tenant*, Role* lifecycle | User lookup, Auth, Tenant query | None | Users/tenants queried frequently (REST), lifecycle changes propagated (Events) |
| **Financial Management** | **Hybrid** | Invoice*, Payment*, JournalEntry* | Balance query, Account lookup | Order*, Goods*, Payment* | Real-time balance queries needed, but posting can be async |
| **Commerce** | **Hybrid** | Order*, Cart*, Product* | Product catalog, Price check | Customer*, Inventory*, Payment* | Product catalog queried constantly, order placement is async workflow |
| **Inventory** | **Hybrid** | StockMovement*, StockReserved* | Stock level query, Location lookup | Order*, Goods*, Production* | Stock levels queried real-time, movements propagated async |
| **Customer Relation** | **Hybrid** | Customer*, Contact*, Campaign* | Customer profile query, History | User*, Order*, Invoice* | Customer data queried for orders, campaign events are async |
| **Manufacturing** | **Hybrid** | ProductionOrder*, QualityCheck* | BOM query, Production status | Order*, Material*, Stock* | BOM queries synchronous, production updates async |
| **Procurement** | **Hybrid** | PurchaseOrder*, GoodsReceived* | Supplier query, PO status | StockMovement*, Invoice* | Supplier lookups sync, procurement workflow async |
| **Operations Service** | **Hybrid** | Ticket*, WorkOrder*, SLA* | Ticket query, Assignment status | Customer*, Product*, User* | Ticket queries sync, status updates propagated async |
| **Business Intelligence** 📊 | **Pure EDA (Consumer)** | Analytics*, Report* (optional) | Dashboard query (own data) | ALL contexts | Consumes all events, no upstream queries needed |
| **Communication Hub** | **REST-Only** | None | Send email, Send SMS | User*, Customer*, Order* (for templates) | Synchronous send/receive, no async workflow |
| **Corporate Services** | **REST-Only** | Asset*, Employee* (low volume) | Asset query, HR data | User* (for employee sync) | Low-volume admin operations, no async benefits |

**Legend:** *EventName* = Event type (e.g., UserCreatedEvent, OrderPlacedEvent)

### 4.2 Per-Context EDA Implementation Guide

#### Pattern A: Hybrid (Recommended for Most Contexts)

**When to Use:**
- ✅ Context needs to query master data from other contexts (e.g., lookup customer, check stock)
- ✅ Context needs to notify other contexts of state changes (e.g., order placed, invoice created)
- ✅ Medium to high integration volume (>10 ops/min)
- ✅ Mixed consistency requirements (immediate reads + eventual writes)

**Implementation Checklist:**
```
☐ Identify read operations (queries)
  → Implement as REST endpoints (GET)
  → Use query-optimized read models
  → Cache frequently accessed data
  
☐ Identify write operations (commands)
  → Publish domain events after successful commits
  → Use transactional outbox pattern
  → Define event schemas (past tense naming)
  
☐ Identify events to consume from other contexts
  → Subscribe to relevant topics
  → Implement idempotent event handlers
  → Maintain local read models if needed
```

**Example: Financial Management Context (Hybrid)**

```kotlin
// 1. REST APIs (Queries) - For immediate consistency
@Path("/api/financial/accounts")
class AccountResource {
    @GET
    @Path("/{accountId}/balance")
    fun getAccountBalance(@PathParam("accountId") accountId: String): Response {
        // Real-time balance query - other contexts call this
        val balance = accountService.getCurrentBalance(AccountId(accountId))
        return Response.ok(balance).build()
    }
}

// 2. Domain Events (Commands) - For async propagation
data class InvoiceGeneratedEvent(
    override val eventId: String,
    override val occurredAt: Instant,
    override val aggregateId: String,  // Invoice ID
    val tenantId: String,
    val customerId: String,
    val orderId: String,
    val amount: Money,
    val dueDate: LocalDate,
) : DomainEvent

// 3. Event Publishing (after writes)
@ApplicationScoped
class InvoiceCommandHandler {
    @Transactional
    fun generateInvoice(command: GenerateInvoiceCommand): Result<Invoice> {
        return invoiceRepository.save(invoice)
            .onSuccess { invoice ->
                // Publish event via outbox
                eventPublisher.publish(InvoiceGeneratedEvent.from(invoice))
                
                // Customer context will send email
                // BI context will update analytics
                // No need for synchronous calls!
            }
    }
}

// 4. Event Consumption (from other contexts)
@ApplicationScoped
class FinancialEventConsumer {
    @Incoming("commerce-events")
    fun handleOrderPlaced(message: Message<String>): CompletionStage<Void> {
        val event = Json.decodeFromString<OrderPlacedEvent>(message.payload)
        
        // Create invoice when order is placed
        return invoiceService.generateInvoice(event)
            .thenRun { message.ack() }
    }
    
    @Incoming("procurement-events")
    fun handleGoodsReceived(message: Message<String>): CompletionStage<Void> {
        val event = Json.decodeFromString<GoodsReceivedEvent>(message.payload)
        
        // Match invoice when goods are received (3-way match)
        return invoiceService.matchInvoice(event)
            .thenRun { message.ack() }
    }
}
```

**Configuration for Hybrid Pattern:**

```yaml
# application.yml - Financial Management Context

# Kafka Channels for Event Publishing
mp:
  messaging:
    outgoing:
      financial-events-out:
        connector: smallrye-kafka
        topic: financial.domain.events.v1
        key.serializer: org.apache.kafka.common.serialization.StringSerializer
        value.serializer: org.apache.kafka.common.serialization.StringSerializer
        partition.count: 3
        
    incoming:
      commerce-events:
        connector: smallrye-kafka
        topic: commerce.domain.events.v1
        group.id: financial-commerce-consumer
        key.deserializer: org.apache.kafka.common.serialization.StringDeserializer
        value.deserializer: org.apache.kafka.common.serialization.StringDeserializer
        enable.auto.commit: false  # Manual ack for idempotency
        
      procurement-events:
        connector: smallrye-kafka
        topic: procurement.domain.events.v1
        group.id: financial-procurement-consumer
        
# REST Client for querying other contexts
quarkus:
  rest-client:
    customer-api:
      url: http://customer-service:8080
      scope: javax.inject.Singleton
    inventory-api:
      url: http://inventory-service:8080
```

#### Pattern B: Pure EDA Consumer (BI/Analytics Only)

**When to Use:**
- ✅ Context only consumes events from other contexts
- ✅ No upstream queries needed (builds own read models)
- ✅ Eventual consistency acceptable (analytics/reporting)
- ✅ High read volume, low write volume

**Implementation Checklist:**
```
☐ Subscribe to ALL relevant domain event topics
☐ Build materialized views/read models locally
☐ Implement idempotent event handlers (event deduplication)
☐ Use event sourcing pattern (optional, for audit trail)
☐ No REST clients needed (self-sufficient)
```

**Example: Business Intelligence Context (Pure EDA)**

```kotlin
// 1. NO REST APIs to other contexts (self-sufficient)

// 2. Event Consumption ONLY
@ApplicationScoped
class BIEventAggregator {
    @Incoming("identity-events")
    fun handleUserEvents(message: Message<String>): CompletionStage<Void> {
        val event = parseEvent(message)
        return when (event) {
            is UserCreatedEvent -> userAnalytics.recordUserCreated(event)
            is UserDeletedEvent -> userAnalytics.recordUserDeleted(event)
            else -> CompletableFuture.completedFuture(null)
        }.thenRun { message.ack() }
    }
    
    @Incoming("commerce-events")
    fun handleCommerceEvents(message: Message<String>): CompletionStage<Void> {
        val event = parseEvent(message)
        return when (event) {
            is OrderPlacedEvent -> orderAnalytics.recordOrder(event)
            is OrderCancelledEvent -> orderAnalytics.recordCancellation(event)
            else -> CompletableFuture.completedFuture(null)
        }.thenRun { message.ack() }
    }
    
    @Incoming("financial-events")
    fun handleFinancialEvents(message: Message<String>): CompletionStage<Void> {
        val event = parseEvent(message)
        return when (event) {
            is InvoiceGeneratedEvent -> revenueAnalytics.recordRevenue(event)
            is PaymentReceivedEvent -> revenueAnalytics.recordPayment(event)
            else -> CompletableFuture.completedFuture(null)
        }.thenRun { message.ack() }
    }
    
    // ... consume from ALL contexts
}

// 3. Local Read Models (materialized views)
@Entity
@Table(name = "bi_order_analytics")
data class OrderAnalyticsView(
    @Id val id: UUID,
    val tenantId: String,
    val orderId: String,
    val customerId: String,
    val orderDate: Instant,
    val totalAmount: BigDecimal,
    val status: String,
    val cancelledAt: Instant?,
    // Denormalized data from multiple events
    val customerName: String?,  // From customer-events
    val productCount: Int?,     // From order-events
    val invoiceId: String?,     // From financial-events
    val paidAt: Instant?,       // From financial-events
)

// 4. Optional: Publish aggregated events for downstream consumers
data class DailyRevenueCalculatedEvent(
    override val eventId: String,
    override val occurredAt: Instant,
    override val aggregateId: String,
    val tenantId: String,
    val date: LocalDate,
    val totalRevenue: Money,
    val orderCount: Int,
) : DomainEvent
```

**Configuration for Pure EDA Consumer:**

```yaml
# application.yml - Business Intelligence Context

mp:
  messaging:
    # NO outgoing channels (pure consumer)
    
    incoming:
      # Subscribe to ALL context events
      identity-events:
        connector: smallrye-kafka
        topic: identity.domain.events.v1
        group.id: bi-identity-consumer
        
      commerce-events:
        connector: smallrye-kafka
        topic: commerce.domain.events.v1
        group.id: bi-commerce-consumer
        
      financial-events:
        connector: smallrye-kafka
        topic: financial.domain.events.v1
        group.id: bi-financial-consumer
        
      inventory-events:
        connector: smallrye-kafka
        topic: inventory.domain.events.v1
        group.id: bi-inventory-consumer
        
      customer-events:
        connector: smallrye-kafka
        topic: customer.domain.events.v1
        group.id: bi-customer-consumer
        
      # ... all other contexts

# NO REST clients (doesn't query other services)
```

#### Pattern C: REST-Only (Low-Volume Admin)

**When to Use:**
- ✅ Low integration volume (<10 ops/min)
- ✅ Synchronous operations only (send email, send SMS)
- ✅ No async workflows or long-running processes
- ✅ Simple request-response pattern sufficient

**Implementation Checklist:**
```
☐ Implement REST endpoints only
☐ Call other services via REST clients (synchronous)
☐ No Kafka configuration needed
☐ Consider circuit breakers for resilience
```

**Example: Communication Hub Context (REST-Only)**

```kotlin
// 1. REST API for synchronous operations
@Path("/api/communication/email")
class EmailResource {
    @Inject
    lateinit var emailService: EmailService
    
    @Inject
    @RestClient
    lateinit var userService: UserRestClient  // Call identity context
    
    @POST
    @Path("/send")
    fun sendEmail(request: SendEmailRequest): Response {
        // Synchronous operation - wait for completion
        val user = userService.getUser(request.userId)  // REST call
        val result = emailService.send(
            to = user.email,
            subject = request.subject,
            body = request.body,
        )
        
        return when (result) {
            is Success -> Response.ok(result.value).build()
            is Failure -> Response.status(500).entity(result.error).build()
        }
    }
}

// 2. NO Kafka configuration needed
// 3. NO event consumers
// 4. REST clients for upstream dependencies

@RegisterRestClient(configKey = "user-api")
interface UserRestClient {
    @GET
    @Path("/api/identity/users/{id}")
    fun getUser(@PathParam("id") userId: String): UserResponse
}
```

### 4.3 Migration Between Patterns

**REST-Only → Hybrid (Most Common Migration):**

```
Phase 1: Add Event Publishing
☐ Add Kafka dependencies
☐ Implement transactional outbox
☐ Publish events for write operations
☐ Keep existing REST APIs (backward compatible)

Phase 2: Add Event Consumption
☐ Subscribe to relevant topics
☐ Implement event handlers
☐ Build local read models (if needed)

Phase 3: Optimize (Optional)
☐ Replace some REST queries with local read models
☐ Monitor performance improvements
☐ Gradually reduce synchronous coupling
```

**Hybrid → Pure EDA (Rare, BI/Analytics Only):**

```
Phase 1: Build Complete Read Models
☐ Ensure all needed data available via events
☐ Build materialized views locally
☐ Test data completeness

Phase 2: Remove REST Clients
☐ Replace synchronous queries with local reads
☐ Deprecate REST client dependencies
☐ Monitor for missing data

Phase 3: Pure Event Consumption
☐ Remove all REST client code
☐ Rely solely on event-driven updates
```

### 4.2 When to Use Events vs REST

**Decision Matrix (from ADR-007):**

| Operation Type | Pattern | Rationale | Example |
|----------------|---------|-----------|---------|
| **Writes/Commands** | Events (Async) | Loose coupling, eventual consistency OK | OrderPlaced, UserCreated |
| **Reads/Queries** | REST (Sync) | Immediate consistency needed | Get user details, Check stock |
| **Notifications** | Events (Async) | Fire-and-forget, multiple subscribers | InvoiceGenerated → Email + Analytics |
| **Real-time Lookups** | REST (Sync) | Low latency required | Validate customer credit limit |
| **Long Workflows** | Events (Choreography) | Decoupled steps | Procure-to-Pay, Order-to-Cash |
| **Master Data** | Hybrid | Query current + propagate changes | Customer master, Product catalog |

**Real ERP Workflow Examples (from EDA_AUDIT_SUMMARY.md):**

#### Order-to-Cash Flow
```
Commerce Context:
  OrderPlacedEvent ───────────────┐
                                  ▼
Inventory Context:           Check Stock (REST) ──► Return availability
  StockReservedEvent ────────────┐
                                  ▼
Financial Context:           Create Invoice
  InvoiceGeneratedEvent ──────────┐
                                  ▼
Customer Context:            Send notification
Payment Context:             Process payment
  PaymentReceivedEvent ───────────┐
                                  ▼
Financial Context:           Post to GL
```

#### Procure-to-Pay Flow
```
Procurement Context:
  PurchaseOrderCreatedEvent ──────┐
                                  ▼
Inventory Context:           Reserve receiving space
  GoodsReceivedEvent ─────────────┐
                                  ▼
Financial Context:           Match invoice
  InvoiceMatchedEvent ────────────┐
                                  ▼
Financial Context:           Schedule payment
  PaymentScheduledEvent ──────────┐
                                  ▼
Financial Context:           Post to AP ledger
```

#### Make-to-Order Flow
```
Commerce Context:
  OrderConfirmedEvent ────────────┐
                                  ▼
Manufacturing Context:       Check BOM (REST) ──► Get bill of materials
  ProductionOrderCreatedEvent ────┐
                                  ▼
Inventory Context:           Reserve materials
  MaterialsIssuedEvent ───────────┐
                                  ▼
Manufacturing Context:       Track production
  ProductionCompletedEvent ───────┐
                                  ▼
Inventory Context:           Receive finished goods
Quality Context:             Inspect quality
```

#### Master Data Synchronization
```
Customer Context:
  CustomerCreatedEvent ───────────┐
                                  ├──► Financial Context (AR setup)
                                  ├──► Commerce Context (Catalog access)
                                  ├──► Manufacturing Context (Custom products)
                                  └──► Business Intelligence (Analytics)
```

### 4.3 EDA Governance Rules (from EDA_AUDIT_SUMMARY.md)

**ArchUnit Tests (Mandatory in CI):**

```kotlin
// tests/arch/EventDrivenArchitectureRules.kt

@ArchTest
val `event publishers must use outbox pattern` = classes()
    .that().implement(EventPublisherPort::class.java)
    .should().haveSimpleName("OutboxEventPublisher")
    .because("Direct Kafka publishing loses events on failures")

@ArchTest
val `domain events must be immutable` = classes()
    .that().implement(DomainEvent::class.java)
    .should().beAnnotatedWith(kotlinx.serialization.Serializable::class.java)
    .andShould().haveOnlyFinalFields()
    .because("Events are facts that cannot change")

@ArchTest
val `application layer must not depend on Kafka` = noClasses()
    .that().resideInAPackage("..application..")
    .should().dependOnClassesThat().resideInAPackage("org.apache.kafka..")
    .because("Event publishing must go through ports")

@ArchTest
val `event publisher port must be in application layer` = classes()
    .that().haveSimpleName("EventPublisherPort")
    .should().resideInAPackage("..application.port.output..")
```

**Naming Conventions:**

| Item | Convention | Example |
|------|------------|---------|
| **Domain Events** | `{Aggregate}{Action}Event` (past tense) | `UserCreatedEvent`, `OrderPlacedEvent` |
| **Kafka Topics** | `{context}.domain.events.v{version}` | `identity.domain.events.v1` |
| **Consumer Groups** | `{context}-{source}-consumer` | `financial-commerce-consumer` |
| **Event Packages** | `{context}.domain.events` | `com.erp.identity.domain.events` |
| **Outbox Table** | `event_outbox` | Standard across all contexts |

**Required Monitoring Metrics:**

```kotlin
// Metrics to add for every context
metricsCollector.counter("${context}.outbox.events.published") // Success count
metricsCollector.counter("${context}.outbox.events.failed")    // Failure count
metricsCollector.gauge("${context}.outbox.pending")            // Unpublished count
metricsCollector.timer("${context}.outbox.publish.duration")   // Processing time
metricsCollector.timer("${context}.outbox.batch.duration")     // Batch processing time
metricsCollector.gauge("${context}.outbox.lag")                // Age of oldest unpublished
```

**Health Checks:**

```kotlin
@Liveness
@ApplicationScoped
class OutboxHealthCheck : HealthCheck {
    override fun call(): HealthCheckResponse {
        val pendingCount = outboxRepository.countUnpublished()
        val oldestEvent = outboxRepository.findOldestUnpublished()
        
        return when {
            pendingCount > 1000 -> HealthCheckResponse.down()
                .withData("pending", pendingCount)
                .withData("reason", "Outbox backlog critical")
                .build()
            
            oldestEvent != null && oldestEvent.age() > Duration.ofMinutes(10) ->
                HealthCheckResponse.down()
                    .withData("lag", oldestEvent.age().toString())
                    .withData("reason", "Outbox lag exceeded threshold")
                    .build()
            
            else -> HealthCheckResponse.up()
                .withData("pending", pendingCount)
                .build()
        }
    }
}
```

### 4.4 Event Implementation Checklist

```
☐ Define domain events (domain layer)
  - Immutable data classes
  - All fields serializable (JSON)
  - Past tense naming
  
☐ Implement transactional outbox
  - OutboxEventEntity (JPA)
  - OutboxEventPublisher (implements EventPublisherPort)
  - OutboxEventScheduler (5-second polling)
  
☐ Configure Kafka channels
  - Topic: {context}.domain.events.v1
  - Partitioning by aggregate ID
  - Headers: event-type, tenant-id, trace-id
  
☐ Implement event consumers (if needed)
  - Idempotent handling (check event ID)
  - ACK/NACK based on processing result
  - DLQ for failed events (max 5 retries)
  
☐ Add metrics and monitoring
  - Events published/failed counters
  - Outbox lag gauge
  - Consumer lag gauge
```

**Complete Event Flow:**
```kotlin
// 1. Domain Event
data class UserCreatedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String,
    val tenantId: String,
    val username: String,
    val email: String,
) : DomainEvent

// 2. Publish via Port
@ApplicationScoped
class UserCommandHandler(
    private val eventPublisher: EventPublisherPort,
) {
    fun createUser(command: CreateUserCommand): Result<User> {
        return userRepository.save(user)
            .onSuccess { user ->
                eventPublisher.publish(UserCreatedEvent(
                    aggregateId = user.id.toString(),
                    tenantId = user.tenantId.toString(),
                    username = user.username,
                    email = user.email,
                ))
            }
    }
}

// 3. Outbox Adapter (Infrastructure)
@ApplicationScoped
class OutboxEventPublisher(
    private val entityManager: EntityManager,
) : EventPublisherPort {
    @Transactional
    override fun publish(event: DomainEvent) {
        val outboxEvent = OutboxEventEntity(
            id = UUID.randomUUID(),
            eventType = event::class.simpleName!!,
            aggregateId = event.aggregateId,
            tenantId = (event as? TenantScoped)?.tenantId,
            payload = Json.encodeToString(event),
            createdAt = Instant.now(),
        )
        entityManager.persist(outboxEvent)
    }
}

// 4. Kafka Publisher (Infrastructure)
@ApplicationScoped
class KafkaOutboxMessagePublisher(
    @Channel("identity-events-out")
    private val emitter: Emitter<String>,
) {
    fun publish(event: OutboxEventEntity): CompletableFuture<Void> {
        val message = Message.of(event.payload)
            .addMetadata(OutgoingKafkaRecordMetadata.builder<String>()
                .withKey(event.aggregateId)
                .withHeaders(RecordHeaders().apply {
                    add("event-type", event.eventType.toByteArray())
                    add("tenant-id", event.tenantId?.toByteArray() ?: byteArrayOf())
                    add("trace-id", MDC.get("traceId")?.toByteArray() ?: byteArrayOf())
                })
                .build())
        
        return emitter.send(message)
            .thenRun {
                Log.infof("Published event %s to Kafka", event.id)
            }
    }
}

// 5. Event Consumer (in other contexts)
@ApplicationScoped
class UserEventConsumer {
    @Incoming("identity-events-in")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    fun consume(message: Message<String>): CompletionStage<Void> {
        val eventType = message.metadata
            .get(IncomingKafkaRecordMetadata::class.java)
            .flatMap { it.getHeaders().lastHeader("event-type") }
            .map { String(it.value()) }
            .orElse("UNKNOWN")
        
        return when (eventType) {
            "UserCreatedEvent" -> handleUserCreated(message)
            else -> CompletableFuture.completedFuture(null)
        }
            .thenRun { message.ack() }
            .exceptionally { error ->
                Log.errorf(error, "Failed to process event")
                message.nack(error)
                null
            }
    }
    
    private fun handleUserCreated(message: Message<String>): CompletionStage<Void> {
        val event = Json.decodeFromString<UserCreatedEvent>(message.payload)
        
        // Idempotent check
        if (eventRepository.exists(event.eventId)) {
            Log.infof("Event %s already processed, skipping", event.eventId)
            return CompletableFuture.completedFuture(null)
        }
        
        // Process event
        return customerService.syncUserData(event)
            .thenRun {
                eventRepository.markProcessed(event.eventId)
                metricsCollector.increment("customer.events.processed", "type", "UserCreated")
            }
    }
}
```

---

## 5. Testing Strategy

### 5.1 Testing Strategy (from ARCHITECTURE.md + Review Cycles)

**Test Pyramid with Architecture Tests:**

```
           ┌──────────────┐
          ╱   E2E (5%)     ╲       • Full system tests
         ┌──────────────────┐      • Real services + DB + Kafka
        ╱  Integration (15%) ╲     • Test adapters with real DB
       ┌──────────────────────┐    • Verify repository implementations
      ╱   Component (10%)      ╲   • Test REST API with mocked services
     ┌──────────────────────────┐
    ╱    Unit Tests (60%)        ╲ • Domain entities, value objects
   └──────────────────────────────┘ • Application services (mocked repos)
   
   ┌──────────────────────────────┐
   │  Architecture Tests (10%)    │ • ArchUnit rules (CI-blocking)
   │  • Layering rules            │ • Platform-shared governance
   │  • Hexagonal architecture    │ • Dependency direction
   │  • Naming conventions        │ • Event-driven patterns
   └──────────────────────────────┘
```

**Test Coverage Targets (from Review Cycles):**

| Layer | Target | Rationale | Current (Tenancy-Identity) |
|-------|--------|-----------|----------------------------|
| Domain Entities | 90%+ | Pure business logic, no I/O | 85% (needs expansion) |
| Domain Services | 85%+ | Complex business rules | 80% (needs expansion) |
| Application Services | 75%+ | Orchestration, some mocking | 70% (in progress) |
| Infrastructure Adapters | 60%+ | Integration tests cover most | 40% (needs work) |
| REST Resources | 80%+ | API contract verification | 75% (good progress) |

**Test Execution Strategy (from test-db-connection.ps1):**

```kotlin
// build.gradle.kts - Convention-with-Override Pattern

tasks.test {
    useJUnitPlatform()
    
    // Default: Run all tests
    // Override with: ./gradlew test -DincludeTags=unit
    systemProperty("includeTags", 
        System.getProperty("includeTags", "unit,integration"))
    
    // Test categories
    systemProperty("junit.jupiter.conditions.deactivate", 
        "org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable")
}

// Fast feedback: Unit tests only (< 2 minutes)
// ./gradlew test -DincludeTags=unit

// Full suite: All tests (< 10 minutes)
// ./gradlew test

// Integration only: Database + Kafka tests
// ./gradlew test -DincludeTags=integration
```

**Architecture Test Categories (CI-Enforced):**

```kotlin
// tests/arch/src/main/kotlin/com/erp/arch/

// 1. Layering Rules (ADR-001)
@ArchTest
val `domain layer must not depend on application layer` = /* ... */

@ArchTest
val `domain layer must not depend on infrastructure layer` = /* ... */

@ArchTest  
val `application layer must not depend on infrastructure layer` = /* ... */

// 2. Hexagonal Architecture Rules
@ArchTest
val `ports must be interfaces in application layer` = /* ... */

@ArchTest
val `adapters must implement ports` = /* ... */

@ArchTest
val `domain entities must not have framework annotations` = /* ... */

// 3. Platform-Shared Governance (ADR-006)
@ArchTest
val `platform-shared must not depend on bounded contexts` = /* ... */

@ArchTest
val `bounded contexts must not depend on each other` = /* ... */

// 4. Event-Driven Architecture (ADR-007)
@ArchTest
val `event publishers must use outbox pattern` = /* ... */

@ArchTest
val `domain events must be immutable` = /* ... */

// 5. Naming Conventions
@ArchTest
val `repositories must end with Repository suffix` = /* ... */

@ArchTest
val `domain events must end with Event suffix` = /* ... */
```

### 5.2 Unit Test Examples

**Domain Entity Tests:**
```kotlin
class UserTest {
    @Test
    fun `authenticate succeeds with valid password`() {
        val crypto = MockPasswordCrypto()
        val user = User.create(/* ... */)
        
        val result = user.authenticate("validPassword", crypto)
        
        assertThat(result).isSuccess()
    }
    
    @Test
    fun `authenticate fails for inactive account`() {
        val user = User.create(/* ... */).copy(status = UserStatus.LOCKED)
        
        val result = user.authenticate("validPassword", crypto)
        
        assertThat(result).isFailure()
        assertThat(result.errorCode()).isEqualTo("ACCOUNT_LOCKED")
    }
}
```

**Application Service Tests:**
```kotlin
@QuarkusTest
class UserCommandHandlerTest {
    @InjectMock
    lateinit var userRepository: UserRepository
    
    @Inject
    lateinit var handler: UserCommandHandler
    
    @Test
    fun `createUser succeeds with valid command`() {
        every { userRepository.save(any()) } returns Result.success(mockUser)
        
        val command = CreateUserCommand(/* ... */)
        val result = handler.handle(command)
        
        assertThat(result).isSuccess()
        verify { userRepository.save(any()) }
    }
    
    @Test
    fun `createUser fails when username exists`() {
        every { userRepository.save(any()) } returns 
            Result.failure("USERNAME_IN_USE", "Username already exists")
        
        val command = CreateUserCommand(/* ... */)
        val result = handler.handle(command)
        
        assertThat(result).isFailure()
        assertThat(result.errorCode()).isEqualTo("USERNAME_IN_USE")
    }
}
```

### 5.3 Integration Test Pattern

```kotlin
@QuarkusTest
@TestProfile(IntegrationTestProfile::class)
class UserIntegrationTest {
    @Inject
    lateinit var userRepository: UserRepository
    
    @BeforeEach
    fun setup() {
        // Clean database before each test
        entityManager.createQuery("DELETE FROM UserEntity").executeUpdate()
    }
    
    @Test
    @Transactional
    fun `save and retrieve user`() {
        val user = User.create(/* ... */)
        
        val saveResult = userRepository.save(user)
        assertThat(saveResult).isSuccess()
        
        val findResult = userRepository.findById(user.tenantId, user.id)
        assertThat(findResult).isSuccess()
        assertThat(findResult.value).isEqualTo(user)
    }
}
```

---

## 6. Platform-Shared Governance (ADR-006)

### 6.1 Decision Tree

```
Is this needed by 2+ bounded contexts?
├─ NO → Put it in the specific bounded context
└─ YES → Is it a technical primitive or business concept?
    ├─ BUSINESS → Duplicate across contexts (different semantics)
    └─ TECHNICAL → Does it contain business rules?
        ├─ YES → Keep in bounded context
        └─ NO → Safe for platform-shared
```

### 6.2 What Belongs in platform-shared

**✅ ALLOWED:**
- `Result<T>`, `DomainError`, `ValidationError` (common-types)
- `Command`, `Query`, `DomainEvent` interfaces (common-types)
- `CorrelationId`, `StructuredLogger` (common-observability)
- `AuthenticationPrincipal`, `SecurityContext` (common-security)
- `EventPublisher`, `EventSubscriber` interfaces (common-messaging)

**❌ FORBIDDEN:**
- Domain models (`Customer`, `Order`, `Invoice`)
- Business logic (`TaxCalculator`, `DiscountPolicy`)
- Value objects with business rules (`Email`, `Address`, `Money`)
- DTOs (`CreateOrderRequest`, `CustomerResponse`)

### 6.3 ArchUnit Enforcement (from ARCH_GOVERNANCE_ROLLOUT_SUMMARY.md)

**CI-Blocking Architecture Tests (Running on Every PR):**

```kotlin
// tests/arch/src/main/kotlin/com/erp/arch/PlatformSharedGovernanceRules.kt

@ArchTest
val `platform-shared must not depend on bounded contexts` = noClasses()
    .that().resideInAPackage("com.erp.shared..")
    .should().dependOnClassesThat().resideInAnyPackage(
        "com.erp.identity..",
        "com.erp.finance..",
        "com.erp.commerce..",
        "com.erp.inventory..",
        "com.erp.customer..",
        "com.erp.manufacturing..",
        "com.erp.procurement..",
        "com.erp.operations..",
        "com.erp.bi..",
        "com.erp.communication..",
        "com.erp.corporate..",
    )
    .because("Platform-shared must remain context-agnostic to prevent distributed monolith")

@ArchTest
val `bounded contexts must not depend on each other directly` = noClasses()
    .that().resideInAPackage("com.erp.identity..")
    .should().dependOnClassesThat().resideInAnyPackage(
        "com.erp.finance..",
        "com.erp.commerce..",
        // ... all other contexts
    )
    .because("Contexts must integrate via events or REST APIs, not direct dependencies")

@ArchTest
val `platform-shared modules must not exceed size limit` = 
    classes().that().resideInAPackage("com.erp.shared..")
        .should().containNumberOfElements(lessThan(500))
        .because("Large platform-shared indicates coupling smell")

@ArchTest
val `only allowed platform-shared modules exist` = 
    noClasses().should().resideInAPackage("com.erp.shared..")
        .andShould().resideOutsideOfPackages(
            "..common.types..",
            "..common.observability..",
            "..common.security..",
            "..common.messaging..",
        )
        .because("Only 4 approved platform-shared modules allowed (ADR-006)")
```

**Local Pre-Commit Hook (Installed Automatically):**

```bash
# .git/hooks/pre-commit

#!/bin/bash
set -e

echo "Running architecture tests..."
./gradlew :tests:arch:test --tests "*PlatformSharedGovernanceRules*" --quiet

if [ $? -ne 0 ]; then
    echo "❌ Architecture tests failed! Fix violations before committing."
    echo "   See: docs/ARCHITECTURE_TESTING_GUIDE.md"
    exit 1
fi

echo "✓ Architecture tests passed"
```

**Weekly Governance Audit (Automated):**

```yaml
# .github/workflows/governance-audit.yml

name: Weekly Governance Audit
on:
  schedule:
    - cron: '0 9 * * MON'  # Every Monday at 9 AM
  workflow_dispatch:  # Manual trigger

jobs:
  audit:
    runs-on: ubuntu-latest
    steps:
      - name: Run All Architecture Tests
        run: ./gradlew :tests:arch:test
      
      - name: Check platform-shared Size
        run: |
          COUNT=$(find platform-shared -name "*.kt" | wc -l)
          if [ $COUNT -gt 100 ]; then
            echo "⚠️ platform-shared has $COUNT files (threshold: 100)"
            exit 1
          fi
      
      - name: Analyze Cross-Context Dependencies
        run: ./scripts/audit-platform-shared.ps1
      
      - name: Post Results to Slack
        if: failure()
        run: |
          curl -X POST $SLACK_WEBHOOK \
            -d '{"text":"🚨 Weekly governance audit failed"}'
```

**Enforcement Metrics (from ARCH_GOVERNANCE_ROLLOUT_SUMMARY.md):**

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Architecture test pass rate | 100% | 100% | ✅ |
| Platform-shared module count | ≤ 4 | 4 | ✅ |
| Platform-shared file count | < 100 | 47 | ✅ |
| Cross-context dependencies | 0 | 0 | ✅ |
| CI enforcement active | Yes | Yes | ✅ |
| Pre-commit hook adoption | 100% | ~80% | 🟡 In progress |

---

## 7. CI/CD & Quality Gates (ADR-008)

**📐 Complete Architecture Details:** See [ADR-008: CI/CD Pipeline Architecture & Network Resilience](adr/ADR-008-cicd-network-resilience.md) for technical decisions, retry strategies, architecture diagrams, and performance metrics.

**📋 Project Timeline:** See [ROADMAP.md Phase 1](../ROADMAP.md#2-phase-1---platform-bootstrap--complete) for CI/CD evolution timeline (v1.0 → v3.0) and delivered artifacts.

### 7.1 Local Development Workflow

**Before Pushing Code:**
```powershell
# 1. Format code
./gradlew ktlintFormat

# 2. Run tests
./gradlew test

# 3. Run architecture tests
./gradlew :tests:arch:test

# 4. Full build
./gradlew build

# 5. Check for errors
./scripts/ci/log-gate.sh scripts/ci/error-allowlist.txt
```

### 7.2 CI Pipeline Overview

```
cache-warmup (10min) → (lint + architecture) →
build (30min) → (integration + security) → status

Expected: 30-32min (cold), 28-30min (warm)
Success Rate: 99%+
Automatic Retry: 95% network failure recovery
```

### 7.3 Understanding CI Failures

**Network Failures (Auto-Retry):**
```
✅ Retry 1/3: Waiting 10 seconds...
✅ Success on attempt 2
```
**Action:** None - automatic retry handles this.

**Code Failures (Fix Required):**
```
❌ BUILD FAILED
> Compilation error: Unresolved reference
```
**Action:** Fix code and push new commit.

**Log Gate Failures:**
```
❌ Found disallowed patterns:
  • ERROR in TenantService.kt:45
```
**Action:** Fix error or add to allowlist if legitimate.

---

## 8. Common Pitfalls & Solutions

### Pitfall 1: Domain Entities with Framework Annotations

**❌ Problem:**
```kotlin
@Entity  // ❌ JPA annotation in domain layer
@Table(name = "users")
data class User(
    @Id val id: UUID,
    @Column(name = "username") val username: String,
)
```

**✅ Solution:** Separate domain models from JPA entities
```kotlin
// Domain layer (pure)
data class User(val id: UserId, val username: String)

// Infrastructure layer
@Entity
@Table(name = "users")
data class UserEntity(@Id val id: UUID, val username: String)
```

### Pitfall 2: Nullable Repository Returns

**❌ Problem:**
```kotlin
interface UserRepository {
    fun findById(id: UserId): User?  // ❌ Loses error context
}
```

**✅ Solution:** Always return Result<T>
```kotlin
interface UserRepository {
    fun findById(id: UserId): Result<User>
}
```

### Pitfall 3: Direct Kafka Publishing

**❌ Problem:**
```kotlin
fun createUser(command: CreateUserCommand): Result<User> {
    return userRepository.save(user)
        .onSuccess { kafkaProducer.send(event) }  // ❌ Event lost if Kafka fails
}
```

**✅ Solution:** Transactional outbox pattern
```kotlin
fun createUser(command: CreateUserCommand): Result<User> {
    return userRepository.save(user)
        .onSuccess { eventPublisher.publish(event) }  // ✅ Outbox guarantees delivery
}
```

### Pitfall 4: Exposing Internal Errors

**❌ Problem:**
```kotlin
return Response.status(500).entity(ErrorResponse(
    message = e.message,  // ❌ Exposes stack trace
    details = mapOf("sql" to e.sqlException)  // ❌ Leaks schema
)).build()
```

**✅ Solution:** Error sanitization
```kotlin
val sanitized = ErrorSanitizer.sanitize(error)
return Response.status(mapStatus(sanitized.code)).entity(ErrorResponse(
    code = sanitized.code,
    message = sanitized.message,  // ✅ Generic message
    details = emptyMap(),  // ✅ No internal details
)).build()
```

### Pitfall 5: Missing Indexes

**❌ Problem:**
```sql
-- No index on foreign key
ALTER TABLE orders ADD COLUMN customer_id UUID;
```

**✅ Solution:** Always index foreign keys
```sql
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
```

### Pitfall 6: Weak Passwords

**❌ Problem:**
```kotlin
val hash = BCrypt.hashpw(password, BCrypt.gensalt())  // ❌ Outdated
```

**✅ Solution:** Argon2id
```kotlin
val hash = argon2.hash(3, 19456, 1, password.toCharArray())  // ✅ Modern
```

### Pitfall 7: Username Enumeration

**❌ Problem:**
```kotlin
val user = userRepository.findByUsername(username)
    ?: return Result.failure("USER_NOT_FOUND")  // ❌ Reveals existence
```

**✅ Solution:** Anti-enumeration with timing guard
```kotlin
val user = userRepository.findByUsername(username).getOrNull()
    ?: run {
        Thread.sleep(100)  // ✅ Constant-time response
        return Result.failure("AUTHENTICATION_FAILED")  // ✅ Generic message
    }
```

### Pitfall 8: No Observability

**❌ Problem:**
```kotlin
fun createUser(command: CreateUserCommand): Result<User> {
    return userRepository.save(User.create(command))  // ❌ No logs, metrics
}
```

**✅ Solution:** Structured logging + metrics
```kotlin
@Counted("identity.user.created")
@Timed("identity.user.create.duration")
fun createUser(command: CreateUserCommand): Result<User> {
    Log.infof("[%s] createUser - tenant=%s", MDC.get("traceId"), command.tenantId)
    
    return userRepository.save(User.create(command))
        .also { result ->
            when (result) {
                is Success -> metricsCollector.increment("identity.user.created.success")
                is Failure -> metricsCollector.increment("identity.user.created.failure")
            }
        }
}
```

---

## 9. Quick Reference

### Implementation Checklist

```
☐ Domain Layer (Start Here)
  ☐ Aggregates/entities (no framework dependencies)
  ☐ Domain events (immutable, past tense)
  ☐ Value objects (type safety)
  
☐ Application Layer
  ☐ Command/query handlers
  ☐ Repository ports (interfaces)
  ☐ Event publisher port
  ☐ DTOs with Bean Validation
  
☐ Infrastructure Layer
  ☐ JPA repositories (Result<T> returns)
  ☐ JPA entities (separate from domain)
  ☐ REST resources (ResultMapper)
  ☐ Transactional outbox pattern
  ☐ Kafka publisher
  ☐ Flyway migrations (with indexes!)
  
☐ Security
  ☐ Argon2id password hashing
  ☐ Anti-enumeration timing guards
  ☐ Error sanitization (production)
  ☐ Rate limiting (auth endpoints)
  
☐ Observability
  ☐ Structured logging (MDC)
  ☐ Metrics (@Counted, @Timed)
  ☐ Correlation IDs (traceId)
  
☐ Testing
  ☐ Unit tests (domain logic)
  ☐ Service tests (mocked repos)
  ☐ Integration tests (real DB)
  ☐ Architecture tests (ArchUnit)
  
☐ CI/CD
  ☐ ktlintFormat before commit
  ☐ Architecture tests pass
  ☐ Log gate clean
  ☐ Security scan (no HIGH CVEs)
```

### Key Files

| File | Purpose |
|------|---------|
| `docs/ARCHITECTURE.md` | System architecture overview |
| `docs/adr/ADR-006-platform-shared-governance.md` | Shared module rules |
| `docs/adr/ADR-007-event-driven-architecture-hybrid-policy.md` | Event vs REST guidance |
| `docs/adr/ADR-008-cicd-network-resilience.md` | CI/CD best practices |
| `docs/ERROR_HANDLING_ANALYSIS_AND_POLICY.md` | Error handling policy |
| `docs/PLATFORM_SHARED_GUIDE.md` | Platform-shared decision tree |
| `docs/REVIEWS_INDEX.md` | Code review learnings |

### Getting Help

- **Architecture Questions:** Review ADRs in `docs/adr/`
- **Implementation Patterns:** See tenancy-identity as reference
- **CI Issues:** `docs/CI_TROUBLESHOOTING.md`
- **Review Feedback:** `docs/REVIEWS_INDEX.md`

---

**Last Updated:** 2025-11-09  
**Next Review:** Quarterly (2026-02-09)  
**Maintained By:** Development Team  
**Version:** 1.0

The CI pipeline automatically retries failed operations caused by network issues:

```yaml
# Retry Configuration (you don't need to do this - it's automatic)
Cache Warmup:  3 attempts, 10s backoff, 6min timeout
Lint:          3 attempts, 10s backoff, 8min timeout
Build:         3 attempts, 15s backoff, 25min timeout
Integration:   3 attempts, 15s backoff, 15min timeout
Architecture:  3 attempts, 10s backoff, 12min timeout
```

**What this means for you:**
- ✅ Network timeouts (ETIMEDOUT, ENETUNREACH) are handled automatically
- ✅ No need to manually re-run failed workflows in most cases
- ✅ Check the logs to see if retries were triggered

**When to manually re-run:**
- ❌ All 3 retry attempts failed (rare)
- ❌ Failure is due to code issues (not network)
- ❌ Job timeout exceeded (extremely rare)

### 2. Interpreting CI Failures

#### Network-Related Failures (Automatically Retried)
```
❌ Error: connect ETIMEDOUT 151.101.xxx.xxx:443
✅ Retry 1/3: Waiting 10 seconds...
✅ Retry 2/3: Waiting 10 seconds...
✅ Success on attempt 2
```

**Action:** None required - the retry mechanism will handle this.

#### Code-Related Failures (Requires Fix)
```
❌ BUILD FAILED
> Task :bounded-contexts:tenancy-identity:domain:compileKotlin FAILED
Compilation error: Unresolved reference: UserService
```

**Action:** Fix the code and push a new commit.

#### Log Gate Failures (Unexpected Errors)
```
❌ Log gate found disallowed patterns:
  • ERROR in TenantService.kt:45 (not in allowlist)
  • NullPointerException in UserResource.kt:89
```

**Action:** Investigate and fix the error, or add to allowlist if legitimate (see section 5).

#### Security Failures (Vulnerabilities Detected)
```
❌ Trivy found 3 HIGH vulnerabilities in dependencies
  • CVE-2024-12345 in org.springframework:spring-core:5.3.9
```

**Action:** Update affected dependencies (see section 6).

### 3. Working with the Cache

#### Cache Behavior
The `cache-warmup` job pre-fetches all dependencies before other jobs run:

```yaml
cache-warmup:
  - Resolves all dependencies (Maven + Gradle)
  - Creates shared cache for downstream jobs
  - Runs once per CI run (not per job)
```

**Benefits:**
- ✅ Eliminates cache race conditions
- ✅ Faster downstream jobs (no dependency downloads)
- ✅ Consistent builds across all jobs

**Cache Invalidation:**
- Automatic when `gradle.properties` or `libs.versions.toml` changes
- Manual via workflow re-run (deletes cache)

**If you suspect cache issues:**
```bash
# Locally verify Gradle cache
./gradlew --refresh-dependencies clean build
```

### 4. Local Development Workflow

#### Before Pushing Code
Run local quality checks to catch issues early:

```powershell
# 1. Run ktlint (style checks)
./gradlew ktlintCheck

# 2. Run build (compilation + tests)
./gradlew build

# 3. Run architecture tests (optional but recommended)
./gradlew :tests:arch:test

# 4. Run log gate (optional)
./scripts/ci/log-gate.sh scripts/ci/error-allowlist.txt
```

#### Quick Local Validation
```powershell
# Fast check (lint + compile only, skip tests)
./gradlew ktlintCheck build -x test

# Full check (everything)
./gradlew ktlintCheck build
```

#### Understanding Local vs CI Differences
| Check | Local | CI |
|-------|-------|-----|
| ktlint | ✅ Same | ✅ Same |
| Build | ✅ Same | ✅ Same + retry |
| Tests | ✅ Same | ✅ Same + retry |
| Log Gate | 🟡 Manual | ✅ Automatic |
| Security Scan | ❌ Not run | ✅ Automatic |
| Architecture Tests | 🟡 Optional | ✅ Automatic |

### 5. Managing Log Gate Allowlist

The log gate scans build outputs for unexpected `ERROR` or `Exception` patterns. Legitimate errors can be added to the allowlist:

#### Allowlist Location
```
scripts/ci/error-allowlist.txt
```

#### When to Add to Allowlist
✅ **Allowed:**
- Expected errors in test code (e.g., testing error handling)
- Known warnings from third-party libraries
- Deprecation warnings that can't be fixed yet

❌ **Not Allowed:**
- Actual runtime errors in production code
- NullPointerExceptions
- Unhandled exceptions
- Build failures

#### How to Add to Allowlist
```bash
# Format: One pattern per line (regex supported)
# Example allowlist entry:
Expected error in ErrorHandlingTest
org.gradle.api.tasks.testing.TestFailure: Expected
```

#### Testing Allowlist Locally
```powershell
# Run log gate manually
./scripts/ci/log-gate.sh scripts/ci/error-allowlist.txt

# If it fails, check the output:
# ❌ Found disallowed patterns:
#   • ERROR in TenantService.kt:45
```

### 6. Managing Dependencies & Security

#### Security Scanning
The CI pipeline runs Trivy security scanning on every build:

```yaml
security:
  - Scans all dependencies for known CVEs
  - Reports uploaded to GitHub Security tab
  - Format: SARIF (GitHub native)
```

#### Handling Vulnerabilities
1. **Review the Security Tab**
   - Go to: Repository → Security → Code scanning
   - Filter by severity: HIGH, CRITICAL

2. **Update Dependencies**
   ```kotlin
   // gradle/libs.versions.toml
   [versions]
   spring-boot = "3.2.0"  # Update to fixed version
   ```

3. **Verify Fix Locally**
   ```powershell
   ./gradlew dependencies --refresh-dependencies
   ./gradlew build
   ```

4. **Push and Wait for CI**
   - Security job will re-scan
   - Check Security tab for cleared alerts

#### Dependency Update Strategy
- **Immediate:** HIGH/CRITICAL CVEs
- **Next Sprint:** MEDIUM CVEs
- **Backlog:** LOW CVEs
- **Never Ignore:** Any CVE in production code

### 7. Performance Optimization Tips

#### Reducing CI Time
1. **Keep PRs Small**
   - Small PRs = faster builds
   - Easier to review and merge
   - Faster feedback cycles

2. **Use Gradle Configuration Cache**
   - Already enabled in CI
   - Run locally to benefit:
     ```powershell
     ./gradlew build --configuration-cache
     ```

3. **Leverage Parallel Execution**
   - Already enabled in CI
   - Run locally:
     ```powershell
     ./gradlew build --parallel
     ```

4. **Use Targeted Builds**
   ```powershell
   # Build only specific module
   ./gradlew :bounded-contexts:tenancy-identity:domain:build

   # Run only specific tests
   ./gradlew :bounded-contexts:tenancy-identity:domain:test --tests "TenantTest"
   ```

#### Understanding Cache Performance
- **Cold Run:** First build after cache invalidation (~30-32min)
- **Warm Run:** Subsequent builds with valid cache (~28-30min)
- **Cache Hit Rate:** Target >90%

**If builds are slow:**
1. Check cache hit rate in CI logs
2. Verify no unnecessary cache invalidation
3. Check for large test suites (consider parallel tests)

### 8. Troubleshooting Common Issues

#### Issue: "Workflow is taking longer than usual"
**Cause:** Cold cache or network issues  
**Solution:** Wait for retry logic to complete (automatic)

#### Issue: "ktlint failures"
**Cause:** Style violations  
**Solution:** 
```powershell
# Auto-fix most issues
./gradlew ktlintFormat

# Check remaining issues
./gradlew ktlintCheck
```

#### Issue: "Build passes locally but fails in CI"
**Cause:** Environment differences  
**Solution:**
1. Check Java version (CI uses Java 21 Temurin)
2. Check Gradle version (CI uses wrapper version)
3. Run with same flags:
   ```powershell
   ./gradlew build --no-daemon --stacktrace
   ```

#### Issue: "Integration tests fail in CI"
**Cause:** PostgreSQL connectivity  
**Solution:**
1. Check PostgreSQL service in CI logs
2. Verify connection string in test config
3. Check for hardcoded localhost (should use service name)

#### Issue: "Security scan found vulnerabilities"
**Cause:** Outdated dependencies  
**Solution:** See section 6 (Managing Dependencies & Security)

---

## Best Practices Summary

### ✅ DO
- Run local checks before pushing (`ktlintCheck`, `build`)
- Keep PRs small and focused (faster CI, easier review)
- Update dependencies promptly when CVEs are found
- Add legitimate errors to log gate allowlist with clear comments
- Review CI logs when failures occur (understand why)
- Use Gradle configuration cache locally (`--configuration-cache`)
- Trust the retry mechanism for network failures

### ❌ DON'T
- Push code without running local checks (wastes CI time)
- Ignore log gate failures (indicates code quality issues)
- Add actual errors to allowlist (defeats the purpose)
- Manually re-run workflows unnecessarily (retry handles most issues)
- Hardcode values that differ between local/CI (use env vars)
- Disable security scanning (required for production)
- Commit large binary files (breaks cache, slows builds)

---

## Emergency Procedures

### Critical CI Failure (Blocking All PRs)
1. **Check CI Status**
   - Go to: Repository → Actions
   - Check for system-wide failures

2. **Identify Root Cause**
   - Network outage? (Rare, automatic retry should handle)
   - GitHub Actions incident? (Check status.github.com)
   - Configuration error? (Recent workflow changes?)

3. **Immediate Actions**
   - **Network issues:** Wait for retry (3 attempts automatic)
   - **GitHub incident:** Wait for resolution (check status page)
   - **Config error:** Revert recent workflow changes

4. **Escalation Path**
   - Contact DevOps team
   - Create incident ticket
   - Notify team in Slack/Teams

### Rollback Procedure
If a workflow change causes issues:

```bash
# 1. Revert workflow changes
git revert <commit-hash>
git push origin main

# 2. Wait for CI to run with reverted config
# 3. Investigate issue offline
# 4. Create fix in new PR with thorough testing
```

---

## Additional Resources

### Core Documentation (Start Here)
- **[Implementation Roadmap](../ROADMAP.md)** - Project timeline, Phase 1 completion, and overall progress tracking
- **[ADR-008: CI/CD Pipeline Architecture](adr/ADR-008-cicd-network-resilience.md)** - Complete architectural decisions, retry strategies, and performance metrics
- **[Reviews Index](REVIEWS_INDEX.md)** - Detailed review cycles for tenancy-identity (Grade A-/A, Batches 1-4)

### CI/CD Documentation
- [CI Evolution Changelog](CI_EVOLUTION_CHANGELOG.md) - Complete version history (v1.0 → v3.0)
- [CI Troubleshooting Guide](CI_TROUBLESHOOTING.md) - Detailed solutions for common issues
- [GitHub Actions Upgrade Guide](GITHUB_ACTIONS_UPGRADE.md) - Technical implementation details
- [GitHub Actions Quick Reference](GITHUB_ACTIONS_QUICKREF.md) - One-page cheat sheet

### External Resources
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
- [Trivy Scanner](https://aquasecurity.github.io/trivy/)
- [ktlint Rules](https://pinterest.github.io/ktlint/)

### Support Channels
- **General Questions:** Team Slack channel
- **CI Issues:** DevOps team
- **Security Alerts:** Security team
- **Emergency:** On-call DevOps engineer

---

## Feedback & Improvements

This advisory is a living document. If you:
- Encounter issues not covered here
- Have suggestions for improvements
- Find outdated information

**Please:**
1. Create a GitHub issue with label `documentation`
2. Suggest updates via PR
3. Share feedback in team retrospectives

---

**Next Review:** 2026-02-09 (Quarterly)  
**Maintained By:** DevOps Team  
**Version:** 3.0
