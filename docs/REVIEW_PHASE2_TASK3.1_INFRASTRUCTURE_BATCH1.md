# Code Review: Phase 2.1 - Identity Domain Infrastructure (Batch 1)

**Roadmap Reference:** Phase 2 - Cross-Cutting Services → Task 3.1 (tenancy-identity implementation)  
**Review Date:** November 6, 2025  
**Reviewer:** Senior Software Engineer  
**Commits Reviewed:** Infrastructure layer (JPA repositories, crypto, outbox, transactions)  
**Overall Grade:** B- (78/100)  
**Status:** Infrastructure Layer - 80% Complete  
**Batch Focus:** Critical infrastructure refinements (outbox publisher, password policy, Result<T> pattern)

---

## 🎯 Critical Refinements Checklist

### Priority 1 - Critical (Do Today) 🔴

#### 1. Outbox Event Publisher
**Status:** ⚠️ Entity exists, scheduler missing  
**Impact:** High - Events not being published  
**Effort:** ~2 hours

**Implementation:**
```kotlin
// Location: identity-infrastructure/.../outbox/OutboxEventScheduler.kt

@ApplicationScoped
class OutboxEventScheduler @Inject constructor(
    private val outboxRepository: OutboxRepository,
    private val kafkaProducer: KafkaProducer,
) {
    @Scheduled(every = "5s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    fun publishPendingEvents() {
        val pending = outboxRepository.findUnpublished(limit = 100)
        
        pending.forEach { event ->
            try {
                kafkaProducer.send(
                    topic = "identity.events",
                    key = event.aggregateId.toString(),
                    value = event.payload
                )
                event.markPublished()
            } catch (e: Exception) {
                Log.errorf(e, "Failed to publish event %s", event.id)
            }
        }
        
        if (pending.isNotEmpty()) {
            Log.infof("Published %d events from outbox", pending.size)
        }
    }
}
```

**Test:**
```bash
# Create user → Check outbox table → Wait 5s → Verify published_at is set
curl -X POST /api/v1/identity/users -d '{...}'
psql -c "SELECT * FROM event_outbox WHERE published_at IS NULL;"
# Wait 5 seconds
psql -c "SELECT * FROM event_outbox WHERE published_at IS NOT NULL;"
```

---

#### 2. Password Policy Enforcement
**Status:** ❌ Not enforced on user creation  
**Impact:** High - Security vulnerability  
**Effort:** ~1 hour

**Implementation:**
```kotlin
// Location: identity-domain/.../model/identity/PasswordPolicy.kt

data class PasswordPolicy(
    val minLength: Int = 8,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireDigit: Boolean = true,
    val requireSpecialChar: Boolean = true,
) {
    fun validate(password: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (password.length < minLength) {
            errors.add("Password must be at least $minLength characters")
        }
        if (requireUppercase && !password.any { it.isUpperCase() }) {
            errors.add("Password must contain uppercase letter")
        }
        if (requireLowercase && !password.any { it.isLowerCase() }) {
            errors.add("Password must contain lowercase letter")
        }
        if (requireDigit && !password.any { it.isDigit() }) {
            errors.add("Password must contain digit")
        }
        if (requireSpecialChar && !password.any { !it.isLetterOrDigit() }) {
            errors.add("Password must contain special character")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    companion object {
        fun strong() = PasswordPolicy(minLength = 12)
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()
}
```

**Usage in handler:**
```kotlin
fun createUser(command: CreateUserCommand): Result<User> {
    val policy = PasswordPolicy.strong()
    return when (val validation = policy.validate(command.password)) {
        is ValidationResult.Valid -> {
            // Continue with user creation
        }
        is ValidationResult.Invalid -> {
            Result.failure(
                code = "WEAK_PASSWORD",
                message = "Password does not meet requirements",
                details = mapOf("errors" to validation.errors)
            )
        }
    }
}
```

---

#### 3. Repository Result<T> Pattern
**Status:** ❌ Still returning nullable types  
**Impact:** High - Inconsistent error handling  
**Effort:** ~2 hours

**Port definition:**
```kotlin
// Location: identity-application/.../port/output/UserRepository.kt

interface UserRepository {
    fun findById(tenantId: TenantId, userId: UserId): Result<User>
    fun findByUsername(tenantId: TenantId, username: String): Result<User>
    fun findByEmail(tenantId: TenantId, email: String): Result<User>
    fun save(user: User): Result<User>
    fun existsByUsername(tenantId: TenantId, username: String): Result<Boolean>
}
```

**JPA implementation:**
```kotlin
override fun findById(tenantId: TenantId, userId: UserId): Result<User> = try {
    val entity = entityManager
        .createQuery("SELECT u FROM UserEntity u WHERE ...", UserEntity::class.java)
        .setParameter("tenantId", tenantId.value)
        .setParameter("userId", userId.value)
        .singleResult
    
    Result.success(mapper.toDomain(entity))
} catch (e: NoResultException) {
    Result.failure("USER_NOT_FOUND", "User not found")
} catch (e: Exception) {
    Log.errorf(e, "Error finding user")
    Result.failure("DATABASE_ERROR", e.message ?: "Unknown error")
}

override fun save(user: User): Result<User> = try {
    val entity = mapper.toEntity(user)
    entityManager.persist(entity)
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
```

**Handler composition:**
```kotlin
fun createUser(command: CreateUserCommand): Result<User> =
    validateTenant(command.tenantId)
        .flatMap { checkUsernameAvailability(command.tenantId, command.username) }
        .flatMap { checkEmailAvailability(command.tenantId, command.email) }
        .flatMap { validatePassword(command.password) }
        .flatMap { createAndSaveUser(command) }
```

---

## Testing Commands

```bash
# Test weak password rejection
curl -X POST /api/v1/identity/users \
  -H "Content-Type: application/json" \
  -d '{"password":"weak", ...}'
# Expected: 400 Bad Request with "WEAK_PASSWORD"

# Test duplicate username
curl -X POST /api/v1/identity/users -d '{"username":"duplicate",...}' # twice
# Expected: 409 Conflict with "USERNAME_IN_USE"

# Check outbox events
SELECT id, event_type, published_at FROM event_outbox ORDER BY created_at DESC LIMIT 10;

# Monitor logs
tail -f logs/application.log | grep "Creating user"
```

---

## Next Steps After Batch 1

- [ ] Add Bean Validation annotations to commands
- [ ] Add structured logging with correlation IDs
- [ ] Add metrics (@Counted, @Timed)
- [ ] Switch to Argon2id for password hashing
- [ ] Add database indexes for performance
- [ ] Write unit tests for adapters

---

**Estimated Total Time:** 5-6 hours  
**Target Completion:** End of day
