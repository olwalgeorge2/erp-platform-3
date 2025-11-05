# ADR-005: Multi-Tenancy Data Isolation Strategy

**Status**: Accepted  
**Date**: 2025-11-05  
**Deciders**: Architecture Team, Security Team  
**Tags**: multi-tenancy, security, data-isolation, compliance

## Context

Our ERP platform will serve multiple customer organizations (tenants) from a shared infrastructure. We must ensure complete data isolation between tenants while maintaining operational efficiency, cost-effectiveness, and regulatory compliance.

## Decision

We will implement a **hybrid multi-tenancy approach** combining:

1. **Row-Level Tenant Discrimination** for primary isolation
2. **Schema-Level Separation** for high-security tenants (optional)
3. **Application-Level Enforcement** as defense-in-depth

### Multi-Tenancy Strategy

#### Tier 1: Standard Tenants (Row-Level Isolation)
- Shared database schemas
- Every table includes `tenant_id` column
- Application enforces tenant filtering on all queries
- Database row-level security (RLS) as additional safety

#### Tier 2: Premium/Regulated Tenants (Schema-Level Isolation)
- Dedicated PostgreSQL schema per tenant
- Physical separation of data
- For compliance-heavy industries (healthcare, finance)
- Higher cost tier

#### Tier 3: Enterprise Tenants (Database-Level Isolation)
- Dedicated database instance
- Complete physical isolation
- Dedicated compute resources
- Highest cost tier, highest security

## Rationale

### Why Row-Level for Most Tenants?
- ✅ **Cost Effective**: Resource sharing reduces infrastructure costs
- ✅ **Operationally Efficient**: Single schema to migrate
- ✅ **Sufficient Isolation**: Application + DB RLS provides strong guarantees
- ✅ **Scalable**: Can support thousands of tenants

### Why Schema/DB-Level Options?
- ✅ **Regulatory Compliance**: HIPAA, GDPR, SOX requirements
- ✅ **Customer Requirements**: Enterprise customers may mandate physical separation
- ✅ **Performance Isolation**: Noisy neighbor protection
- ✅ **Data Residency**: Can place in specific regions

## Consequences

### Positive
- ✅ Flexible pricing tiers based on isolation level
- ✅ Most tenants share resources (cost efficient)
- ✅ High-value tenants get dedicated resources
- ✅ Clear upgrade path between tiers
- ✅ Compliance-ready for regulated industries
- ✅ Defense-in-depth security

### Negative
- ❌ More complex deployment logic
- ❌ Different code paths for different tiers
- ❌ Need to manage multiple isolation strategies
- ❌ Testing must cover all isolation modes
- ❌ Tenant migration between tiers is complex

### Neutral
- ⚖️ Requires careful query auditing
- ⚖️ Need comprehensive access control testing
- ⚖️ Monitoring must be tenant-aware

## Implementation Details

### Database Schema Design

#### Row-Level Isolation (Tier 1)

```sql
-- Every table includes tenant_id
CREATE TABLE financial_accounting.accounts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,  -- Foreign key to tenants table
    account_number VARCHAR(50) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    
    UNIQUE(tenant_id, account_number)
);

-- Index for efficient tenant filtering
CREATE INDEX idx_accounts_tenant_id ON financial_accounting.accounts(tenant_id);

-- PostgreSQL Row-Level Security (RLS)
ALTER TABLE financial_accounting.accounts ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON financial_accounting.accounts
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);
```

#### Application-Level Enforcement

```kotlin
// Tenant Context (Request-Scoped)
@RequestScoped
class TenantContext {
    private var tenantId: TenantId? = null
    
    fun setTenantId(id: TenantId) {
        this.tenantId = id
    }
    
    fun getTenantId(): TenantId {
        return tenantId ?: throw IllegalStateException("Tenant context not set")
    }
}

// Tenant Filter (Applied to all requests)
@Provider
@Priority(Priorities.AUTHENTICATION + 1)
class TenantFilter(
    private val tenantContext: TenantContext,
    private val jwtValidator: JwtValidator
) : ContainerRequestFilter {
    
    override fun filter(requestContext: ContainerRequestContext) {
        val token = extractToken(requestContext)
        val claims = jwtValidator.validate(token)
        
        // Extract and set tenant ID from JWT
        tenantContext.setTenantId(TenantId(claims.tenantId))
    }
}

// Base Repository with Tenant Filtering
@MappedSuperclass
abstract class TenantAwareRepository<T : TenantAwareEntity>(
    private val tenantContext: TenantContext
) : PanacheRepositoryBase<T, UUID> {
    
    // Override find methods to include tenant filter
    override fun findById(id: UUID): T? {
        return find("id = ?1 and tenantId = ?2", id, tenantContext.getTenantId())
            .firstResult()
    }
    
    override fun listAll(): List<T> {
        return find("tenantId = ?1", tenantContext.getTenantId()).list()
    }
    
    // Prevent cross-tenant access
    fun save(entity: T) {
        if (entity.tenantId != tenantContext.getTenantId()) {
            throw SecurityException("Cannot save entity for different tenant")
        }
        persist(entity)
    }
}

// Domain Entity
@Entity
@Table(name = "accounts", schema = "financial_accounting")
abstract class Account(
    @Column(name = "tenant_id", nullable = false)
    val tenantId: TenantId,
    
    @Column(name = "account_number", nullable = false)
    val accountNumber: String,
    
    // ... other fields
) : TenantAwareEntity
```

#### Database Session Configuration

```kotlin
@ApplicationScoped
class TenantAwareDataSource(
    private val tenantContext: TenantContext
) {
    
    @Produces
    @RequestScoped
    fun getConnection(dataSource: DataSource): Connection {
        val connection = dataSource.connection
        
        // Set PostgreSQL session variable for RLS
        val statement = connection.prepareStatement(
            "SET app.current_tenant_id = ?"
        )
        statement.setObject(1, tenantContext.getTenantId().value)
        statement.execute()
        
        return connection
    }
}
```

### Schema-Level Isolation (Tier 2)

```kotlin
// Dynamic schema routing
@ApplicationScoped
class SchemaRouter(
    private val tenantContext: TenantContext,
    private val tenantRepository: TenantRepository
) {
    
    fun getSchemaName(): String {
        val tenant = tenantRepository.findById(tenantContext.getTenantId())
        
        return when (tenant.isolationTier) {
            IsolationTier.STANDARD -> "financial_accounting"  // Shared schema
            IsolationTier.PREMIUM -> "tenant_${tenant.id}_financial_accounting"  // Dedicated schema
            IsolationTier.ENTERPRISE -> throw IllegalStateException("Enterprise tenants use separate DB")
        }
    }
}

// Configure Hibernate to use dynamic schema
@ApplicationScoped
class DynamicSchemaProvider implements CurrentTenantIdentifierResolver {
    
    @Inject
    lateinit var schemaRouter: SchemaRouter
    
    override fun resolveCurrentTenantIdentifier(): String {
        return schemaRouter.getSchemaName()
    }
    
    override fun validateExistingCurrentSessions(): Boolean = false
}
```

### Tenant Onboarding

```kotlin
@ApplicationScoped
class TenantProvisioningService(
    private val dataSource: DataSource,
    private val flywayMigrationRunner: FlywayMigrationRunner
) {
    
    @Transactional
    fun provisionTenant(request: ProvisionTenantRequest): TenantId {
        val tenantId = TenantId.generate()
        
        // Create tenant record
        val tenant = Tenant(
            id = tenantId,
            name = request.organizationName,
            isolationTier = request.isolationTier,
            status = TenantStatus.PROVISIONING
        )
        entityManager.persist(tenant)
        
        when (request.isolationTier) {
            IsolationTier.STANDARD -> {
                // No additional setup - uses shared schema
                logger.info("Tenant $tenantId using shared schema")
            }
            
            IsolationTier.PREMIUM -> {
                // Create dedicated schema
                createDedicatedSchema(tenantId)
                runMigrations(tenantId)
            }
            
            IsolationTier.ENTERPRISE -> {
                // Provision dedicated database (async job)
                provisionDedicatedDatabase(tenantId)
            }
        }
        
        tenant.status = TenantStatus.ACTIVE
        return tenantId
    }
    
    private fun createDedicatedSchema(tenantId: TenantId) {
        val schemaName = "tenant_${tenantId}_financial_accounting"
        dataSource.connection.use { conn ->
            conn.createStatement().execute(
                "CREATE SCHEMA IF NOT EXISTS $schemaName"
            )
        }
    }
}
```

## Security Measures

### 1. Defense in Depth
- ✅ JWT contains tenant ID (cannot be tampered)
- ✅ Application enforces tenant context on every query
- ✅ Database RLS as safety net
- ✅ Architecture tests verify tenant filtering

### 2. Audit Logging
```kotlin
@ApplicationScoped
class TenantAuditLogger {
    
    fun logDataAccess(
        tenantId: TenantId,
        userId: UserId,
        resource: String,
        action: String
    ) {
        auditLog.info(
            "Tenant ${tenantId} | User ${userId} | ${action} ${resource}"
        )
    }
}
```

### 3. Cross-Tenant Access Prevention

```kotlin
// Architecture test
@ArchTest
fun `repositories must extend TenantAwareRepository`(importedClasses: JavaClasses) {
    classes()
        .that().haveSimpleNameEndingWith("Repository")
        .should().beAssignableTo(TenantAwareRepository::class.java)
        .check(importedClasses)
}

// Runtime validation
@Aspect
class TenantSecurityAspect(private val tenantContext: TenantContext) {
    
    @Before("execution(* com.erp..repository..*(..))")
    fun validateTenantContext(joinPoint: ProceedingJoinPoint) {
        if (tenantContext.getTenantId() == null) {
            throw SecurityException("Tenant context not set - potential security breach")
        }
    }
}
```

## Monitoring & Compliance

### Metrics
```kotlin
- tenant_data_access_total{tenant, resource, action}
- tenant_cross_access_attempts_total  // Should always be 0
- tenant_query_duration_seconds{tenant}
- tenant_data_size_bytes{tenant, context}
```

### Compliance Reports
- Monthly tenant isolation audit
- Cross-tenant access attempts log
- Data residency compliance report
- Encryption at rest verification

## Testing Strategy

### Unit Tests
```kotlin
@Test
fun `repository should only return data for current tenant`() {
    // Setup
    tenantContext.setTenantId(TenantId("tenant-a"))
    val accountA = Account(tenantId = TenantId("tenant-a"), ...)
    val accountB = Account(tenantId = TenantId("tenant-b"), ...)
    repository.persist(accountA, accountB)
    
    // Execute
    val results = repository.listAll()
    
    // Assert
    assertThat(results).hasSize(1)
    assertThat(results[0].tenantId).isEqualTo(TenantId("tenant-a"))
}
```

### Integration Tests
```kotlin
@Test
fun `RLS should prevent cross-tenant queries`() {
    // This test uses raw SQL to verify RLS works even if app logic fails
    dataSource.connection.use { conn ->
        conn.prepareStatement("SET app.current_tenant_id = 'tenant-a'").execute()
        
        val results = conn.prepareStatement(
            "SELECT * FROM accounts"
        ).executeQuery()
        
        // Should only see tenant-a accounts
        while (results.next()) {
            assertThat(results.getString("tenant_id")).isEqualTo("tenant-a")
        }
    }
}
```

### Penetration Testing
- Attempt JWT manipulation to access other tenant data
- SQL injection attempts to bypass tenant filters
- API fuzzing with invalid tenant IDs

## Migration Between Tiers

```kotlin
@ApplicationScoped
class TenantMigrationService {
    
    @Transactional
    fun migrateToHigherTier(
        tenantId: TenantId, 
        targetTier: IsolationTier
    ): MigrationJob {
        
        val job = MigrationJob(tenantId, targetTier)
        
        // 1. Create new schema/database
        // 2. Copy data
        // 3. Verify integrity
        // 4. Switch routing
        // 5. Delete old data (after grace period)
        
        return job
    }
}
```

## Alternatives Considered

### 1. Database Per Tenant (All Tenants)
**Rejected**: Cost prohibitive, operational nightmare with thousands of tenants.

### 2. Application-Only Filtering (No RLS)
**Rejected**: Single point of failure, too risky if app has bug.

### 3. Separate Applications Per Tenant
**Rejected**: Doesn't scale, maintenance nightmare, no resource sharing.

## Related ADRs

- ADR-002: Database Per Bounded Context
- ADR-004: API Gateway Pattern
- ADR-007: Authentication and Authorization Strategy (to be written)

## Review Date

- **Before Phase 3**: Complete security audit of tenant isolation
- **Before Production**: Penetration testing
- **Quarterly**: Review tenant tier distribution and migration needs
