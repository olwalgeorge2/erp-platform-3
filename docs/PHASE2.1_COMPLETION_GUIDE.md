# Phase 2.1 Completion Guide - Kafka, Argon2, Indexes, Tests

**Reference:** Phase 2 Task 3.1 (tenancy-identity implementation)  
**Status:** Batch 1 & 2 Complete, Final refinements in progress

---

## ✅ Kafka Integration (COMPLETED)

### Files Created/Updated:
1. ✅ `KafkaOutboxMessagePublisher.kt` - Kafka producer with metrics
2. ✅ `OutboxEventScheduler.kt` - Added MeterRegistry metrics
3. ✅ `application.properties` - Kafka channel configuration
4. ✅ `build.gradle.kts` - Added `quarkus-messaging-kafka`

### Configuration Details:
- **Topic:** `identity.domain.events.v1`
- **Partition Key:** `aggregateId` (ensures ordering per aggregate)
- **Headers:** `event-type`, `trace-id`, `tenant-id`, `aggregate-id`
- **Idempotence:** Enabled
- **Acks:** `all` (strongest durability)
- **Retries:** 3 with idempotence
- **Compression:** Snappy

### Metrics Added:
- `identity.outbox.events.published{outcome=success}` - Published events counter
- `identity.outbox.events.published{outcome=failure}` - Failed events counter
- `identity.outbox.batch.duration` - Batch processing timer
- `identity.outbox.publish.attempts` - Individual publish attempts (from publisher)
- `identity.outbox.publish.duration` - Individual publish duration

### Testing:
```bash
# Start Kafka locally (Docker)
docker run -d --name kafka \
  -p 9092:9092 \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  apache/kafka:latest

# Consume events
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic identity.domain.events.v1 \
  --property print.headers=true \
  --from-beginning

# Check metrics
curl http://localhost:8080/metrics | grep identity.outbox
```

---

## 🔒 Argon2id Crypto Upgrade

### Implementation Summary:
- `Argon2idCredentialCryptoAdapter` now provides Argon2id hashing with secure defaults.
- PBKDF2 hashing/verification is retained for legacy credentials with constant-time comparisons.
- `UserCommandHandler` explicitly requests `HashAlgorithm.ARGON2` for new and reset passwords.
- Unit coverage: `Argon2idCredentialCryptoAdapterTest` exercises Argon2 and PBKDF2 flows.

### Migration Notes:
- Existing PBKDF2 credentials continue to verify successfully and will be rehashed to Argon2id on next password change.
- Once migration is complete, PBKDF2 support can be removed in a future iteration.

### References:
- Dependency: `de.mkammerer:argon2-jvm:2.11`
- Implementation: `bounded-contexts/tenancy-identity/identity-infrastructure/src/main/kotlin/com.erp.identity.infrastructure/crypto/Argon2idCredentialCryptoAdapter.kt`
- Tests: `bounded-contexts/tenancy-identity/identity-infrastructure/src/test/kotlin/com/erp/identity/infrastructure/crypto/Argon2idCredentialCryptoAdapterTest.kt`

### Migration Strategy:
1. `Credential.algorithm` persists the hashing scheme per record (existing PBKDF2 rows remain valid).
2. Any password change invokes `HashAlgorithm.ARGON2`, transparently upgrading stored credentials.
3. Monitor adoption and remove PBKDF2 fallback once all credentials rotate to Argon2id.

---

## 📊 Database Indexes

### Current Schema Analysis:
- **Unique constraints:** ✅ `(tenant_id, username)`, `(tenant_id, email)`
- **Primary keys:** ✅ All entities
- **Missing indexes:** Lookup fields without constraints

### Recommended Indexes:

**File:** `V003__add_performance_indexes.sql`

```sql
-- User lookups by status (for admin dashboards)
CREATE INDEX idx_users_tenant_status 
ON identity_users(tenant_id, status) 
WHERE status != 'DELETED';

-- User lookups by creation time (recent users queries)
CREATE INDEX idx_users_tenant_created_at 
ON identity_users(tenant_id, created_at DESC);

-- Role lookups by tenant
CREATE INDEX idx_roles_tenant 
ON identity_roles(tenant_id);

-- Outbox event processing queries
CREATE INDEX idx_outbox_pending_events 
ON identity_outbox_events(status, recorded_at) 
WHERE status = 'PENDING';

CREATE INDEX idx_outbox_failed_events 
ON identity_outbox_events(status, failure_count, last_attempt_at) 
WHERE status = 'PENDING' AND failure_count > 0;

-- Tenant lookups by slug (public-facing URLs)
CREATE UNIQUE INDEX idx_tenants_slug 
ON identity_tenants(LOWER(slug));

-- Analyze tables for query planner
ANALYZE identity_users;
ANALYZE identity_roles;
ANALYZE identity_tenants;
ANALYZE identity_outbox_events;
```

### Testing Indexes:

```sql
-- Explain query plans before/after
EXPLAIN ANALYZE 
SELECT * FROM identity_users 
WHERE tenant_id = 'a1b2c3d4-...' AND status = 'ACTIVE';

-- Should show "Index Scan using idx_users_tenant_status"
```

---

## 🧪 Unit Tests

### Test Structure:

```
identity-domain/src/test/kotlin/
  ├── model/
  │   ├── PasswordPolicyTest.kt
  │   ├── UserTest.kt
  │   └── TenantTest.kt
  └── services/
      └── AuthenticationServiceTest.kt

identity-infrastructure/src/test/kotlin/
  ├── crypto/
  │   ├── Argon2idCredentialCryptoAdapterTest.kt
  │   └── Argon2idCredentialCryptoAdapterTest.kt
  ├── persistence/
  │   ├── JpaUserRepositoryTest.kt
  │   ├── JpaTenantRepositoryTest.kt
  │   └── JpaRoleRepositoryTest.kt
  └── outbox/
      ├── OutboxEventSchedulerTest.kt
      └── KafkaOutboxMessagePublisherTest.kt
```

### Example Test: PasswordPolicyTest

```kotlin
package com.erp.identity.domain.model.identity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PasswordPolicyTest {
    
    @Test
    fun `strong policy should reject short passwords`() {
        val policy = PasswordPolicy.strong()
        val errors = policy.validate("Short1!")
        
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.field == "password" && it.message.contains("12 characters") })
    }
    
    @Test
    fun `strong policy should reject passwords without uppercase`() {
        val policy = PasswordPolicy.strong()
        val errors = policy.validate("lowercase123!")
        
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.message.contains("uppercase") })
    }
    
    @Test
    fun `strong policy should accept valid passwords`() {
        val policy = PasswordPolicy.strong()
        val errors = policy.validate("ValidPassword123!")
        
        assertTrue(errors.isEmpty())
    }
}
```

### Example Test: Argon2idCredentialCryptoAdapterTest

```kotlin
package com.erp.identity.infrastructure.crypto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class Argon2idCredentialCryptoAdapterTest {
    
    private val adapter = Argon2idCredentialCryptoAdapter()
    
    @Test
    fun `should hash password successfully`() {
        val plainPassword = "MySecurePassword123!"
        val hash = adapter.hashPassword(plainPassword)
        
        assertNotNull(hash)
        assertNotEquals(plainPassword, hash)
        assertTrue(hash.startsWith("\$argon2id\$"))
    }
    
    @Test
    fun `should verify correct password`() {
        val plainPassword = "MySecurePassword123!"
        val hash = adapter.hashPassword(plainPassword)
        
        assertTrue(adapter.verifyPassword(plainPassword, hash))
    }
    
    @Test
    fun `should reject incorrect password`() {
        val plainPassword = "MySecurePassword123!"
        val hash = adapter.hashPassword(plainPassword)
        
        assertFalse(adapter.verifyPassword("WrongPassword", hash))
    }
    
    @Test
    fun `should generate different hashes for same password`() {
        val plainPassword = "MySecurePassword123!"
        val hash1 = adapter.hashPassword(plainPassword)
        val hash2 = adapter.hashPassword(plainPassword)
        
        assertNotEquals(hash1, hash2) // Different salts
        assertTrue(adapter.verifyPassword(plainPassword, hash1))
        assertTrue(adapter.verifyPassword(plainPassword, hash2))
    }
}
```

### Run Tests:

```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :bounded-contexts:tenancy-identity:identity-domain:test
./gradlew :bounded-contexts:tenancy-identity:identity-infrastructure:test

# Generate coverage report
./gradlew test jacocoTestReport

# View report
open build/reports/jacoco/test/html/index.html
```

---

## 📋 Completion Checklist

### Kafka Transport (TODAY)
- [x] Add `quarkus-messaging-kafka` dependency
- [x] Create `KafkaOutboxMessagePublisher`
- [x] Add metrics to `OutboxEventScheduler`
- [x] Configure Kafka channel in `application.properties`
- [x] Update CDI configuration (wire Kafka publisher instead of logging)
- [ ] Test with local Kafka instance (manual step pending)
- [ ] Verify events published with headers

### Argon2id Upgrade (NEXT)
- [ ] Add `argon2-jvm` dependency
- [ ] Implement `Argon2idCredentialCryptoAdapter`
- [ ] Add unit tests for Argon2id adapter
- [ ] Add migration strategy for existing passwords
- [ ] Update documentation

### Database Indexes (AFTER ARGON2)
- [ ] Create Flyway migration `V003__add_performance_indexes.sql`
- [ ] Apply migration to dev database
- [ ] Verify index usage with EXPLAIN ANALYZE
- [ ] Document index strategy

### Unit Tests (PARALLEL)
- [ ] PasswordPolicy tests
- [ ] User/Tenant domain model tests
- [ ] AuthenticationService tests
- [x] Crypto adapter tests (Argon2id with PBKDF2 fallback)
- [ ] Repository tests (using Testcontainers)
- [ ] Outbox scheduler tests

### Integration Tests (FINAL)
- [ ] Command handler end-to-end tests
- [ ] Outbox processing with Kafka (Testcontainers)
- [ ] Transaction rollback scenarios
- [ ] Constraint violation handling

---

## 🎯 Next Session Focus

**Immediate (This Session):**
1. Wire `KafkaOutboxMessagePublisher` in CDI configuration
2. Test Kafka publishing locally
3. Verify metrics in Prometheus

**Tomorrow:**
1. Implement Argon2id adapter
2. Add crypto unit tests
3. Start database index migration

**This Week:**
1. Complete all unit tests (target: 80% coverage)
2. Add integration tests for critical paths
3. Wrap Phase 2.1 with documentation update

---

## 📊 Estimated Timeline

| Task | Effort | Status |
|------|--------|--------|
| Kafka Integration | 2-3 hours | 🔄 90% (CDI wiring pending) |
| Argon2id Upgrade | 2-3 hours | 📋 Planned |
| Database Indexes | 1 hour | 📋 Planned |
| Unit Tests | 4-6 hours | 📋 Planned |
| Integration Tests | 3-4 hours | 📋 Planned |
| **Total** | **12-17 hours** | **🎯 Phase 2.1 completion** |

---

**After Phase 2.1 → Phase 2.2: API Gateway & OpenAPI Contracts**
