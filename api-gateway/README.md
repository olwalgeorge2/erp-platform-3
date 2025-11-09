# API Gateway

**Status:** 🚧 Sprint 3 Implementation (Starting Nov 11, 2025)  
**Version:** 0.1.0-SNAPSHOT  
**Tech Stack:** Quarkus 3.29.0, Kotlin 2.2.0, Redis, JWT  

## Overview

The API Gateway serves as the single entry point for all external client requests to the ERP platform. It provides routing, authentication, rate limiting, and observability for the microservices architecture.

**Key Responsibilities:**
- ✅ Request routing to bounded contexts
- ✅ JWT authentication and tenant resolution
- ✅ Per-tenant and per-endpoint rate limiting
- ✅ Cross-cutting concerns (logging, tracing, metrics)
- ✅ CORS handling and security enforcement
- ✅ Error standardization

**Related Documentation:**
- 📐 [ADR-004: API Gateway Pattern](../docs/adr/ADR-004-api-gateway-pattern.md)
- 📋 [Sprint 3 Implementation Plan](../docs/SPRINT3_API_GATEWAY_PLAN.md)
- 🔒 [Developer Advisory - Security Patterns](../docs/DEVELOPER_ADVISORY.md)
- 🏗️ [Architecture Overview](../docs/ARCHITECTURE.md#api-gateway-api-gateway)

---

## Pre-Implementation Checklist

### ✅ Dependencies (Completed 2025-11-09)

All required dependencies added to `gradle/libs.versions.toml`:

**Quarkus Extensions (managed by BOM):**
- ✅ `quarkus-rest` - REST endpoints
- ✅ `quarkus-rest-jackson` - JSON serialization
 - ✅ `quarkus-rest-client` - Service forwarding (non-reactive)
- ✅ `quarkus-rest-client-jackson` - REST client JSON
- ✅ `quarkus-smallrye-jwt` - JWT validation
- ✅ `quarkus-smallrye-jwt-build` - JWT utilities
- ✅ `quarkus-redis-client` - Rate limiting backend
- ✅ `quarkus-micrometer-registry-prometheus` - Metrics
- ✅ `quarkus-opentelemetry` - Distributed tracing
- ✅ `quarkus-logging-json` - Structured logging

**Test Dependencies:**
- ✅ `testcontainers-core` (1.20.1)
- ✅ `testcontainers-junit` (1.20.1)
- ✅ `wiremock` (3.9.1)
- ✅ `rest-assured` (5.5.0)

### 📋 Placeholder Files to Replace

Current placeholder structure:
```
src/main/kotlin/com/erp/apigateway/
├── ApiGatewayApplication.kt (placeholder) ❌ REPLACE
├── routing/
│   ├── GatewayRouter.kt (placeholder) ❌ REPLACE
│   └── RouteDefinitions.kt ❌ REPLACE
└── security/
    ├── GatewaySecurityConfig.kt (placeholder) ❌ REPLACE
    └── TenantFilter.kt (placeholder) ❌ REPLACE
```

**Action Required:** Do NOT supplement placeholders—replace them entirely with production implementations.

---

## Implementation Roadmap

### Epic 0: Pre-Sprint Setup ✅
- [x] Version catalog dependencies added
- [x] Test dependencies configured
- [x] Placeholder inventory completed
- [x] Sprint plan updated with security requirements

### Epic 1: Core Gateway Infrastructure (33 hours)
**Stories:** 1.1 → 1.4  
**Focus:** Configuration, routing, request forwarding, error handling

**Key Files:**
```
config/
  ├── RouteConfiguration.kt
  ├── ServiceTarget.kt
  └── PublicEndpointsConfig.kt
routing/
  ├── RouteResolver.kt
  └── RouteNotFoundException.kt
proxy/
  ├── ProxyService.kt
  ├── ProxyController.kt
  ├── RequestForwarder.kt
  └── ResponseMapper.kt
exception/
  ├── GatewayExceptionMapper.kt
  └── [typed exceptions]
```

### Epic 2: Authentication & Authorization (27 hours)
**Stories:** 2.1 → 2.5  
**Focus:** JWT validation, tenant context, CORS, security hardening

**Security Priorities (Story 2.5):**
- ⚠️ Anti-enumeration patterns (generic 401 responses)
- ⚠️ Timing guards (prevent side-channel attacks)
- ⚠️ ArchUnit tests (enforce architecture boundaries)
- ⚠️ Audit logging (structured, PII-free)

**Key Files:**
```
security/
  ├── AuthenticationFilter.kt
  ├── JwtValidator.kt
  ├── GatewaySecurityContext.kt
  ├── AuthenticationTimingGuard.kt
  ├── AntiEnumerationHandler.kt
  └── SecurityAuditLogger.kt
context/
  ├── TenantContext.kt
  └── TenantContextFilter.kt
test/arch/
  └── GatewayArchitectureTest.kt
```

**Reference:** DEVELOPER_ADVISORY.md sections 1-7 for security patterns proven in tenancy-identity implementation.

### Epic 3: Rate Limiting (16 hours)
**Stories:** 3.1 → 3.3  
**Focus:** Redis integration, token bucket algorithm, rate limit enforcement

**Key Files:**
```
infrastructure/
  └── RedisService.kt
ratelimit/
  ├── RateLimiter.kt
  ├── RateLimitConfig.kt
  ├── RateLimitResult.kt
  └── TokenBucketAlgorithm.kt
filter/
  └── RateLimitFilter.kt
```

### Epic 4: Observability (8 hours)
**Stories:** 4.1 → 4.3  
**Focus:** Structured logging, Prometheus metrics, distributed tracing

**Key Files:**
```
logging/
  └── RequestLoggingFilter.kt
metrics/
  └── GatewayMetrics.kt
```

---

## Configuration

### Environment Variables

Reference `.env.example` for baseline:
```bash
# Gateway
API_GATEWAY_PORT=8080
API_GATEWAY_HOST=localhost

# Identity Service
IDENTITY_SERVICE_URL=http://localhost:8081

# JWT
JWT_PUBLIC_KEY_URL=http://localhost:8081/api/v1/identity/.well-known/jwks.json
JWT_ISSUER=erp-platform

# Redis
REDIS_URL=redis://localhost:6379
REDIS_PASSWORD=

# Rate Limiting
RATE_LIMIT_DEFAULT_REQUESTS=100
RATE_LIMIT_DEFAULT_WINDOW=60s

# CORS
CORS_ORIGINS=http://localhost:3000,https://app.example.com

# Observability
QUARKUS_LOG_LEVEL=INFO
QUARKUS_LOG_CONSOLE_JSON=true
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
```

### application.yml Structure

```yaml
quarkus:
  http:
    port: ${API_GATEWAY_PORT:8080}
    host: ${API_GATEWAY_HOST:0.0.0.0}
    cors:
      origins: ${CORS_ORIGINS:http://localhost:3000}
      methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
      headers: Authorization,Content-Type,X-Trace-Id,X-Tenant-Id,X-User-Id
      exposed-headers: Location,X-Total-Count,X-RateLimit-Limit,X-RateLimit-Remaining
      access-control-max-age: 3600
  
  redis:
    hosts: ${REDIS_URL:redis://localhost:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 10s
  
  log:
    console:
      json: ${QUARKUS_LOG_CONSOLE_JSON:true}
      json.pretty-print: false
    level: ${QUARKUS_LOG_LEVEL:INFO}

mp.jwt.verify:
  publickey.location: ${JWT_PUBLIC_KEY_URL}
  issuer: ${JWT_ISSUER}

gateway:
  services:
    tenancy-identity:
      url: ${IDENTITY_SERVICE_URL:http://localhost:8081}
      timeout: 5s
      retries: 2
  
  rate-limits:
    default:
      requests-per-minute: ${RATE_LIMIT_DEFAULT_REQUESTS:100}
      window: ${RATE_LIMIT_DEFAULT_WINDOW:60s}
```

---

## Security Best Practices

### 1. Anti-Enumeration Patterns
❌ **DON'T:**
```kotlin
// Bad: Discloses user existence
if (userNotFound) throw NotFoundException("User admin@example.com not found")
if (invalidPassword) throw UnauthorizedException("Invalid password")
```

✅ **DO:**
```kotlin
// Good: Generic message for all auth failures
throw UnauthorizedException("AUTHENTICATION_FAILED")
```

### 2. Timing Guards
Normalize response times to prevent timing side-channel attacks:

```kotlin
@ApplicationScoped
class AuthenticationTimingGuard {
    private val minResponseTimeMs = 100L
    
    suspend fun executeWithTimingGuard(block: suspend () -> Response): Response {
        val start = System.currentTimeMillis()
        val result = try {
            block()
        } catch (e: Exception) {
            // Still apply timing guard on failures
            throw e
        } finally {
            val elapsed = System.currentTimeMillis() - start
            val remaining = minResponseTimeMs - elapsed
            if (remaining > 0) delay(remaining)
        }
        return result
    }
}
```

### 3. Architecture Enforcement
ArchUnit tests in `test/arch/GatewayArchitectureTest.kt`:

```kotlin
@AnalyzeClasses(packages = ["com.erp.apigateway"])
class GatewayArchitectureTest {
    
    @ArchTest
    val `no business logic in gateway` = 
        noClasses()
            .that().resideInAPackage("..gateway..")
            .should().dependOnClassesThat().resideInAPackage("..domain..")
    
    @ArchTest
    val `filters execute in correct order` =
        classes()
            .that().areAnnotatedWith(Provider::class.java)
            .and().implement(ContainerRequestFilter::class.java)
            .should().beAnnotatedWith(Priority::class.java)
}
```

### 4. Audit Logging (PII-Free)
```kotlin
logger.info {
    "authentication_attempt" to mapOf(
        "tenantId" to tenantId,
        "userId" to userId,  // Use ID, not username/email
        "traceId" to traceId,
        "result" to "success",
        "duration_ms" to duration
        // ❌ NO credentials, tokens, or PII
    )
}
```

**Reference:** DEVELOPER_ADVISORY.md for complete security patterns proven in tenancy-identity (Grade A-).

---

## Testing Strategy

### Unit Tests (>80% coverage target)
- Route resolution logic
- JWT validation scenarios
- Rate limiter algorithms
- Exception mapping
- Security timing guards

### Integration Tests
- WireMock for backend service mocking
- Testcontainers for Redis
- REST Assured for API testing
- CORS validation
- Public/protected endpoint access

### Load Tests
**Target:** 1000 req/s per instance, p95 latency < 50ms

```javascript
// k6 script (see SPRINT3_API_GATEWAY_PLAN.md:826)
export let options = {
    stages: [
        { duration: '1m', target: 100 },
        { duration: '5m', target: 1000 },
        { duration: '1m', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<100'],
        http_req_failed: ['rate<0.01'],
    },
};
```

---

## Integration with Tenancy-Identity

### JWT Token Flow
```
1. Client → POST /api/v1/identity/auth/login
2. Identity Service → Returns JWT
3. Client → GET /api/v1/{context}/{resource} (Authorization: Bearer <JWT>)
4. Gateway → Validates JWT with identity service JWKS
5. Gateway → Extracts tenantId, userId, roles from claims
6. Gateway → Sets X-Tenant-Id, X-User-Id headers
7. Gateway → Forwards to bounded context
```

### Tenant Context Propagation
```kotlin
// Gateway adds headers for downstream services
headers["X-Tenant-Id"] = jwtClaims.tenantId
headers["X-User-Id"] = jwtClaims.userId
headers["X-Trace-Id"] = correlationId

// MDC for logging
MDC.put("tenantId", tenantId)
MDC.put("userId", userId)
MDC.put("traceId", traceId)
```

---

## Build & Run

### Local Development
```powershell
# Build
./gradlew :api-gateway:build

# Run in dev mode (hot reload)
./gradlew :api-gateway:quarkusDev

# Run tests
./gradlew :api-gateway:test

# Run integration tests
./gradlew :api-gateway:test --tests "*IntegrationTest"
```

### Docker
```powershell
# Build native image
./gradlew :api-gateway:build -Dquarkus.package.type=native

# Run container
docker run -p 8080:8080 -e API_GATEWAY_PORT=8080 api-gateway:latest
```

---

## Monitoring & Operations

### Health Checks
- **Liveness:** `GET /health/live`
- **Readiness:** `GET /health/ready` (includes Redis check)

### Metrics
- **Prometheus:** `GET /metrics`
- **Key Metrics:**
  - `gateway_requests_total` (counter)
  - `gateway_request_duration_seconds` (histogram)
  - `gateway_errors_total` (counter)
  - `gateway_ratelimit_exceeded_total` (counter)

### Tracing
- Correlation IDs via `X-Trace-Id` header
- OpenTelemetry spans for:
  - Route resolution
  - Authentication
  - Rate limit checks
  - Proxy requests

---

## Current Status

**Phase:** Pre-Implementation  
**Sprint:** Sprint 3 (Nov 11-25, 2025)  
**Next Steps:**
1. ✅ Dependencies verified
2. ✅ Sprint plan finalized
3. 🔄 Story 1.1: Project Setup (Day 1)
4. 📋 Epic 1-4 implementation (Days 1-10)

**Quality Gates:**
- [ ] CI/CD pipeline green
- [ ] >80% test coverage
- [ ] ktlint passing
- [ ] Security scan clean
- [ ] ArchUnit tests enforcing architecture
- [ ] Load test achieving 1000 req/s

---

## Support & Contribution

**Documentation:**
- Sprint plan: `docs/SPRINT3_API_GATEWAY_PLAN.md`
- Architecture decisions: `docs/adr/ADR-004-api-gateway-pattern.md`
- Security patterns: `docs/DEVELOPER_ADVISORY.md`

**Team:**
- Developer 1: Core infrastructure, authentication
- Developer 2: Rate limiting, observability

**Review Process:**
Following tenancy-identity review patterns (achieved Grade A- through 4 review cycles).
