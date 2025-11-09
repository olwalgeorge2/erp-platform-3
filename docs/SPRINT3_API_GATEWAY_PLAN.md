# Sprint 3: API Gateway Implementation Plan

**Sprint Duration:** 2 weeks (10 working days)  
**Start Date:** 2025-11-11  
**Target Completion:** 2025-11-22  
**Sprint Goal:** Deliver production-ready API Gateway with routing, authentication, rate limiting, and observability

**Dependencies:**
- ✅ Phase 1: CI/CD Pipeline (Complete)
- ✅ Phase 2 Task 3.1: Tenancy-Identity (85% complete, sufficient for gateway integration)
- ✅ Platform-shared modules available

**Related Documentation:**
- 📐 [ADR-004: API Gateway Pattern](adr/ADR-004-api-gateway-pattern.md)
- 📚 [Developer Advisory](DEVELOPER_ADVISORY.md) - Sections 1-7 for implementation patterns
- 📋 [Architecture](ARCHITECTURE.md#api-gateway-api-gateway) - Gateway responsibilities
- 🔍 [Roadmap Phase 2 Task 3.2](ROADMAP.md#3-phase-2---cross-cutting-services--in-progress)

---

## Table of Contents

1. [Sprint Overview](#sprint-overview)
2. [Success Criteria](#success-criteria)
3. [Work Breakdown Structure](#work-breakdown-structure)
4. [Implementation Stories](#implementation-stories)
5. [Technical Design](#technical-design)
6. [Testing Strategy](#testing-strategy)
7. [Risk Management](#risk-management)
8. [Daily Milestones](#daily-milestones)

---

## Sprint Overview

### Objectives

**Primary Goals:**
1. ✅ Request routing to bounded contexts (tenancy-identity initially)
2. ✅ JWT authentication and tenant resolution
3. ✅ Rate limiting (per-tenant, per-endpoint)
4. ✅ Observability (logging, tracing, metrics)
5. ✅ Error standardization and CORS handling

**Secondary Goals (Nice-to-Have):**
- Circuit breaker integration
- Request/response transformation
- API versioning strategy
- GraphQL gateway spike

### Scope

**In Scope:**
- Core gateway routing infrastructure
- Authentication filter with JWT validation
- Integration with tenancy-identity context
- Rate limiting with Redis backend
- Distributed tracing with correlation IDs
- Prometheus metrics collection
- Comprehensive test coverage (unit, integration, E2E)
- CI/CD integration
- Production deployment configuration

**Out of Scope:**
- OAuth2/OIDC provider integration (Phase 3)
- API composition/aggregation (Phase 4)
- GraphQL gateway (Phase 5)
- WebSocket support (Future)
- Service mesh migration (Future)

### Team Allocation

**Estimated Effort:** 80 person-hours (2 developers × 10 days)

**Roles:**
- **Developer 1:** Core routing, authentication, configuration
- **Developer 2:** Rate limiting, observability, testing
- **Both:** Integration testing, documentation, deployment

---

## Success Criteria

### Functional Requirements

| Requirement | Acceptance Criteria | Priority |
|------------|---------------------|----------|
| **Request Routing** | Route requests to tenancy-identity context with < 5ms overhead | P0 |
| **Authentication** | Validate JWT tokens, reject invalid/expired tokens | P0 |
| **Tenant Resolution** | Extract tenantId from token, set context for downstream services | P0 |
| **Rate Limiting** | Enforce per-tenant limits (100 req/min default), return 429 on exceed | P0 |
| **Error Handling** | Return consistent error responses matching tenancy-identity format | P0 |
| **CORS** | Handle preflight requests, configurable allowed origins | P0 |
| **Health Checks** | Expose /health/live and /health/ready endpoints | P1 |
| **Metrics** | Expose Prometheus metrics for requests, latency, errors | P1 |
| **Tracing** | Propagate correlation IDs through all requests | P1 |

### Non-Functional Requirements

| Requirement | Target | Measurement |
|------------|--------|-------------|
| **Performance** | p95 latency < 50ms | Load testing |
| **Availability** | 99.9% uptime | Monitoring |
| **Throughput** | 1000 req/s per instance | Load testing |
| **Test Coverage** | > 80% overall | Kover report |
| **Code Quality** | ktlint passing, no critical SonarQube issues | CI pipeline |
| **Documentation** | All endpoints documented, README complete | Manual review |

### Quality Gates

**Before Sprint Closure:**
- ✅ All P0 stories complete with tests
- ✅ CI/CD pipeline green (lint, build, test, security)
- ✅ Integration tests passing with real tenancy-identity service
- ✅ Load test results documented (1000 req/s achieved)
- ✅ Security scan clean (no HIGH/CRITICAL vulnerabilities)
- ✅ Code review approved by senior engineer
- ✅ Deployment runbook created

---

## Work Breakdown Structure

### Epic 0: Pre-Sprint Setup (1 hour)

#### Story 0.1: Dependency & Placeholder Cleanup (1 hour)
**Estimate:** 1 hour  
**Assignee:** Developer 1  
**Priority:** P0 - MUST complete before Story 1.1

**Tasks:**
- [x] Add missing Quarkus extensions to `gradle/libs.versions.toml`:
  - `quarkus-rest-client`
  - `quarkus-rest-client-jackson`
  - `quarkus-smallrye-jwt`
  - `quarkus-smallrye-jwt-build`
  - `quarkus-redis-client`
  - `quarkus-micrometer-registry-prometheus`
- [x] Add test dependencies to version catalog:
  - `testcontainers` (core + junit-jupiter)
  - `wiremock`
  - `rest-assured`
- [ ] Review existing placeholder files to be replaced:
  - `ApiGatewayApplication.kt`
  - `routing/GatewayRouter.kt`
  - `security/GatewaySecurityConfig.kt`
  - `security/TenantFilter.kt`

**Acceptance Criteria:**
- Version catalog entries added (versions managed by Quarkus BOM)
- Test dependencies available for integration tests
- Placeholder file inventory documented

**Files Modified:**
```
gradle/libs.versions.toml (update)
```

**Notes:**
- Quarkus BOM manages all Quarkus extension versions automatically
- Focus on gateway implementation, not version management

---

### Epic 1: Core Gateway Infrastructure (33 hours)

#### Story 1.1: Project Setup & Configuration (5 hours)
**Estimate:** 5 hours (updated from 4 hours)  
**Assignee:** Developer 1

**Tasks:**
- [x] Review existing `api-gateway/` structure
- [ ] Update `build.gradle.kts` with dependencies from version catalog
- [ ] Replace placeholder `ApiGatewayApplication.kt` with proper Quarkus app
- [ ] Create `application.yml` configuration structure:
  - Server config (port: 8080)
  - CORS settings
  - JWT validation settings
  - Redis connection
  - Observability (logging, metrics, tracing)
- [ ] Create `config/api-gateway.env` with deployment variables
- [ ] Reference `.env.example` for port/host configurations
- [ ] Verify build passes: `./gradlew :api-gateway:build`
- [ ] Update README.md with:
  - Architecture overview
  - Setup instructions
  - Configuration reference
  - Integration points with tenancy-identity

**Acceptance Criteria:**
- Gateway compiles successfully with all dependencies
- Configuration files align with `.env.example` variables
- Placeholder files replaced (not supplemented)
- README documents security patterns from DEVELOPER_ADVISORY
- Build passes without warnings

**Files to Create/Modify:**
```
api-gateway/
├── src/main/kotlin/com/erp/apigateway/ApiGatewayApplication.kt (replace)
├── src/main/resources/application.yml (create)
├── config/api-gateway.env (create)
├── README.md (update)
└── build.gradle.kts (update)
```

**Security Notes:**
- Reference DEVELOPER_ADVISORY.md sections 1-7 for patterns
- Anti-enumeration for auth failures (generic 401 responses)
- Timing guards for authentication to prevent side-channel attacks

---

#### Story 1.2: Service Registry & Route Configuration (8 hours)
**Estimate:** 8 hours  
**Assignee:** Developer 1

**Tasks:**
- [ ] Create `RouteConfiguration` class for service registry
- [ ] Implement URL pattern matching logic
- [ ] Create `ServiceTarget` data class (url, timeout, retries)
- [ ] Add route configuration to `application.yml`:
  ```yaml
  gateway:
    services:
      tenancy-identity:
        url: ${IDENTITY_SERVICE_URL:http://localhost:8081}
        timeout: 5s
        retries: 2
  ```
- [ ] Implement dynamic route resolution
- [ ] Add health check endpoints for registered services
- [ ] Create unit tests for route matching
- [ ] Handle route conflicts and fallbacks

**Acceptance Criteria:**
- Routes resolve correctly for `/api/v1/identity/*` patterns
- Service targets configurable via environment variables
- Unit tests cover all route matching scenarios (>90% coverage)
- Invalid routes return 404 with helpful error message

**Files to Create:**
```
api-gateway/src/main/kotlin/
├── config/
│   ├── RouteConfiguration.kt
│   └── ServiceTarget.kt
├── routing/
│   ├── RouteResolver.kt
│   └── RouteNotFoundException.kt
└── test/
    └── routing/RouteResolverTest.kt
```

---

#### Story 1.3: HTTP Client & Request Forwarding (12 hours)
**Estimate:** 12 hours  
**Assignee:** Developer 1

**Tasks:**
- [ ] Implement `ProxyService` using Quarkus REST Client
- [ ] Forward HTTP method (GET, POST, PUT, DELETE, PATCH)
- [ ] Forward request headers (except Host, Connection)
- [ ] Forward request body (JSON, form-data)
- [ ] Forward query parameters
- [ ] Handle response streaming (large responses)
- [ ] Add timeout handling (per-service configuration)
- [ ] Add retry logic with exponential backoff
- [ ] Implement circuit breaker (optional, Resilience4j)
- [ ] Create `ProxyController` to handle incoming requests
- [ ] Add request/response logging
- [ ] Create integration tests with WireMock

**Acceptance Criteria:**
- All HTTP methods forwarded correctly
- Headers preserved (excluding hop-by-hop headers)
- Timeouts respected (5s default, configurable)
- Retries work with exponential backoff (2 attempts)
- Integration tests verify forwarding with mock backend
- Large responses (>1MB) stream correctly

**Files to Create:**
```
api-gateway/src/main/kotlin/
├── proxy/
│   ├── ProxyService.kt
│   ├── ProxyController.kt
│   ├── RequestForwarder.kt
│   └── ResponseMapper.kt
├── client/
│   └── DynamicRestClient.kt
└── test/
    └── proxy/ProxyServiceTest.kt
```

---

#### Story 1.4: Error Handling & Response Standardization (8 hours)
**Estimate:** 8 hours  
**Assignee:** Developer 1

**Tasks:**
- [ ] Create `GatewayExceptionMapper` for global error handling
- [ ] Map common exceptions to HTTP status codes:
  - `RouteNotFoundException` → 404
  - `AuthenticationException` → 401
  - `AuthorizationException` → 403
  - `RateLimitExceededException` → 429
  - `ServiceUnavailableException` → 503
  - `TimeoutException` → 504
- [ ] Create consistent error response format matching identity service:
  ```json
  {
    "code": "ROUTE_NOT_FOUND",
    "message": "No route found for /api/v1/unknown",
    "timestamp": "2025-11-11T10:30:00Z",
    "traceId": "abc123"
  }
  ```
- [ ] Add correlation ID to all error responses
- [ ] Log errors with appropriate severity (WARN for 4xx, ERROR for 5xx)
- [ ] Create unit tests for all exception mappings

**Acceptance Criteria:**
- All exceptions return consistent JSON format
- Status codes match HTTP semantics
- Correlation IDs included in all responses
- Error messages are user-friendly (no stack traces)
- Tests cover all exception types

**Files to Create:**
```
api-gateway/src/main/kotlin/
├── exception/
│   ├── GatewayExceptionMapper.kt
│   ├── GatewayException.kt
│   ├── RouteNotFoundException.kt
│   ├── ServiceUnavailableException.kt
│   └── RateLimitExceededException.kt
└── dto/
    └── ErrorResponse.kt
```

---

### Epic 2: Authentication & Authorization (24 hours)

#### Story 2.1: JWT Validation Filter (12 hours)
**Estimate:** 12 hours  
**Assignee:** Developer 1

**Tasks:**
- [ ] Configure `quarkus-smallrye-jwt` extension
- [ ] Create `AuthenticationFilter` (ContainerRequestFilter)
- [ ] Extract JWT from Authorization header: `Bearer <token>`
- [ ] Validate JWT signature with public key from identity service
- [ ] Validate token expiration (`exp` claim)
- [ ] Extract claims: `tenantId`, `userId`, `username`, `roles`
- [ ] Set `SecurityContext` with authenticated principal
- [ ] Handle missing/invalid/expired tokens → 401
- [ ] Add correlation ID to MDC
- [ ] Create unit tests with mocked JWT validation
- [ ] Create integration tests with real JWT tokens

**JWT Configuration:**
```yaml
mp.jwt.verify.publickey.location: ${JWT_PUBLIC_KEY_URL:http://localhost:8081/api/v1/identity/.well-known/jwks.json}
mp.jwt.verify.issuer: ${JWT_ISSUER:erp-platform}
```

**Acceptance Criteria:**
- Valid JWTs pass authentication
- Invalid/expired JWTs return 401 with clear error
- Claims extracted and available in SecurityContext
- Public key fetched from identity service on startup
- Tests cover all validation scenarios

**Files to Create:**
```
api-gateway/src/main/kotlin/
├── security/
│   ├── AuthenticationFilter.kt
│   ├── GatewaySecurityContext.kt
│   └── JwtValidator.kt
└── test/
    └── security/AuthenticationFilterTest.kt
```

---

#### Story 2.2: Tenant Context Resolution (6 hours)
**Estimate:** 6 hours  
**Assignee:** Developer 1

**Tasks:**
- [ ] Create `TenantContext` class (request-scoped bean)
- [ ] Extract `tenantId` from JWT claims
- [ ] Set `X-Tenant-Id` header for downstream services
- [ ] Extract `userId` and set `X-User-Id` header
- [ ] Create `TenantContextFilter` to run after authentication
- [ ] Add MDC context for logging:
  ```kotlin
  MDC.put("tenantId", tenantId)
  MDC.put("userId", userId)
  MDC.put("traceId", traceId)
  ```
- [ ] Create unit tests for context resolution

**Acceptance Criteria:**
- Tenant ID extracted from JWT correctly
- Headers added to all downstream requests
- MDC context set for all log entries
- Tests verify header propagation

**Files to Create:**
```
api-gateway/src/main/kotlin/
├── context/
│   ├── TenantContext.kt
│   └── TenantContextFilter.kt
└── test/
    └── context/TenantContextTest.kt
```

---

#### Story 2.3: CORS Configuration (4 hours)
**Estimate:** 4 hours  
**Assignee:** Developer 2

**Tasks:**
- [ ] Configure CORS in `application.yml`:
  ```yaml
  quarkus.http.cors:
    origins: ${CORS_ORIGINS:http://localhost:3000}
    methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
    headers: Authorization,Content-Type,X-Trace-Id
    exposed-headers: Location,X-Total-Count
    access-control-max-age: 3600
  ```
- [ ] Test preflight OPTIONS requests
- [ ] Verify CORS headers in responses
- [ ] Create integration tests for CORS scenarios

**Acceptance Criteria:**
- Preflight requests return 200 with correct headers
- Configured origins allowed, others blocked
- All necessary headers exposed

**Files to Modify:**
```
api-gateway/src/main/resources/application.yml
api-gateway/src/test/kotlin/cors/CorsTest.kt
```

---

#### Story 2.4: Public Endpoints Configuration (2 hours)
**Estimate:** 2 hours  
**Assignee:** Developer 1

**Tasks:**
- [ ] Define public endpoints (no authentication required):
  - `/health/*` (health checks)
  - `/metrics` (Prometheus)
  - `/api/v1/identity/auth/login` (login endpoint)
  - `/api/v1/identity/auth/register` (registration)
- [ ] Update `AuthenticationFilter` to skip public endpoints
- [ ] Create configuration for public path patterns
- [ ] Add tests for public endpoint access

**Acceptance Criteria:**
- Public endpoints accessible without JWT
- Protected endpoints require valid JWT
- Configuration easily extensible

**Files to Create:**
```
api-gateway/src/main/kotlin/
└── config/PublicEndpointsConfig.kt
```

---

#### Story 2.5: Security Hardening (3 hours)
**Estimate:** 3 hours  
**Assignee:** Developer 1  
**Priority:** P0 - Security critical

**Tasks:**
- [ ] Implement anti-enumeration patterns from DEVELOPER_ADVISORY.md:
  - Generic 401 responses (no user existence disclosure)
  - Consistent error messages for auth failures
- [ ] Add timing guards for authentication:
  - Minimum response time budget (prevent timing attacks)
  - Normalize successful vs failed auth response times
- [ ] Create ArchUnit tests for gateway architecture:
  - Validate filter execution order
  - Ensure security context propagation
  - Verify no business logic in gateway layer
- [ ] Validate compliance with ADR-006 platform-shared rules:
  - No business domain models in gateway
  - Only technical primitives and security utilities
- [ ] Security logging without PII exposure:
  - Log authentication attempts (without credentials)
  - Structured logging for audit trails
  - Correlation IDs for security events

**Acceptance Criteria:**
- Authentication failures return generic messages
- Timing attacks mitigated with response budgets
- ArchUnit tests enforce architecture boundaries
- Security audit logs structured and PII-free
- Aligns with tenancy-identity security patterns

**Files to Create:**
```
api-gateway/src/main/kotlin/
├── security/
│   ├── AuthenticationTimingGuard.kt
│   ├── AntiEnumerationHandler.kt
│   └── SecurityAuditLogger.kt
└── test/
    ├── arch/GatewayArchitectureTest.kt
    └── security/SecurityHardeningTest.kt
```

**Reference Documentation:**
- DEVELOPER_ADVISORY.md (Sections 1-7)
- ADR-006: Platform-Shared Governance Rules
- REVIEW_PHASE1_ERROR_SANITIZATION_* (Anti-enumeration patterns)

---

### Epic 3: Rate Limiting (16 hours)

#### Story 3.1: Redis Integration (4 hours)
**Estimate:** 4 hours  
**Assignee:** Developer 2

**Tasks:**
- [ ] Add Redis dependency to `build.gradle.kts`
- [ ] Configure Redis connection in `application.yml`:
  ```yaml
  quarkus.redis.hosts: ${REDIS_URL:redis://localhost:6379}
  quarkus.redis.password: ${REDIS_PASSWORD:}
  quarkus.redis.timeout: 10s
  ```
- [ ] Create `RedisService` wrapper for rate limit operations
- [ ] Test Redis connection on startup
- [ ] Add health check for Redis connectivity
- [ ] Create integration test with Testcontainers Redis

**Acceptance Criteria:**
- Redis connects successfully
- Health check reports Redis status
- Integration tests use Testcontainers

**Files to Create:**
```
api-gateway/src/main/kotlin/
├── infrastructure/
│   └── RedisService.kt
└── test/
    └── infrastructure/RedisServiceTest.kt
```

---

#### Story 3.2: Rate Limiter Implementation (8 hours)
**Estimate:** 8 hours  
**Assignee:** Developer 2

**Tasks:**
- [ ] Implement token bucket algorithm with Redis
- [ ] Create `RateLimiter` service:
  ```kotlin
  fun checkLimit(tenantId: String, endpoint: String): RateLimitResult
  ```
- [ ] Redis key structure: `ratelimit:{tenantId}:{endpoint}:{window}`
- [ ] Use Redis INCR with TTL for sliding window
- [ ] Configurable limits per tenant and endpoint
- [ ] Return remaining quota in response headers:
  ```
  X-RateLimit-Limit: 100
  X-RateLimit-Remaining: 87
  X-RateLimit-Reset: 1699789200
  ```
- [ ] Create unit tests with mocked Redis
- [ ] Create integration tests with real Redis

**Acceptance Criteria:**
- Rate limits enforced correctly (100 req/min default)
- Sliding window algorithm accurate
- Headers include quota information
- Tests verify limit enforcement

**Files to Create:**
```
api-gateway/src/main/kotlin/
├── ratelimit/
│   ├── RateLimiter.kt
│   ├── RateLimitConfig.kt
│   ├── RateLimitResult.kt
│   └── TokenBucketAlgorithm.kt
└── test/
    └── ratelimit/RateLimiterTest.kt
```

---

#### Story 3.3: Rate Limit Filter (4 hours)
**Estimate:** 4 hours  
**Assignee:** Developer 2

**Tasks:**
- [ ] Create `RateLimitFilter` (ContainerRequestFilter)
- [ ] Run after authentication filter (priority = AUTHENTICATION + 100)
- [ ] Extract tenantId from context
- [ ] Check rate limit for endpoint
- [ ] If exceeded, throw `RateLimitExceededException` → 429
- [ ] Add rate limit headers to all responses
- [ ] Create integration tests for rate limiting

**Acceptance Criteria:**
- Requests within limit succeed
- Requests exceeding limit return 429
- Headers present in all responses
- Integration tests verify end-to-end flow

**Files to Create:**
```
api-gateway/src/main/kotlin/
├── filter/
│   └── RateLimitFilter.kt
└── test/
    └── filter/RateLimitFilterTest.kt
```

---

### Epic 4: Observability (8 hours)

#### Story 4.1: Structured Logging (3 hours)
**Estimate:** 3 hours  
**Assignee:** Developer 2

**Tasks:**
- [ ] Configure JSON logging in `application.yml`:
  ```yaml
  quarkus.log.console.json: true
  quarkus.log.console.json.pretty-print: false
  ```
- [ ] Create `RequestLoggingFilter` for access logs
- [ ] Log all requests with: method, path, status, duration, tenantId, userId, traceId
- [ ] Use MDC for correlation context
- [ ] Configure log levels (INFO for access, DEBUG for details)
- [ ] Test log format with sample requests

**Acceptance Criteria:**
- All requests logged in JSON format
- Logs include correlation context
- Log format parseable by log aggregators

**Files to Create:**
```
api-gateway/src/main/kotlin/
└── logging/
    └── RequestLoggingFilter.kt
```

---

#### Story 4.2: Metrics Collection (3 hours)
**Estimate:** 3 hours  
**Assignee:** Developer 2

**Tasks:**
- [ ] Configure Micrometer with Prometheus registry
- [ ] Add metrics to `ProxyService`:
  - `gateway_requests_total` (counter by endpoint, method, status)
  - `gateway_request_duration_seconds` (histogram by endpoint)
  - `gateway_errors_total` (counter by error type)
  - `gateway_ratelimit_exceeded_total` (counter by tenant)
- [ ] Expose metrics at `/metrics` endpoint
- [ ] Create Prometheus scrape configuration example
- [ ] Test metrics with sample requests

**Acceptance Criteria:**
- Metrics exposed at `/metrics` in Prometheus format
- Counters and histograms track all requests
- Metrics include useful labels (endpoint, method, tenant)

**Files to Create:**
```
api-gateway/src/main/kotlin/
└── metrics/
    └── GatewayMetrics.kt
```

---

#### Story 4.3: Distributed Tracing (2 hours)
**Estimate:** 2 hours  
**Assignee:** Developer 2

**Tasks:**
- [ ] Configure OpenTelemetry exporter
- [ ] Generate/propagate trace IDs via `X-Trace-Id` header
- [ ] Create spans for gateway operations:
  - `gateway.route-resolution`
  - `gateway.authentication`
  - `gateway.rate-limit-check`
  - `gateway.proxy-request`
- [ ] Propagate trace context to downstream services
- [ ] Test trace propagation with sample requests

**Acceptance Criteria:**
- Trace IDs generated for all requests
- Spans created for key operations
- Trace context propagated to backend services

**Files to Modify:**
```
api-gateway/src/main/resources/application.yml
api-gateway/src/main/kotlin/proxy/ProxyService.kt
```

---

## Technical Design

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        External Clients                      │
│                    (Web, Mobile, 3rd Party)                 │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTPS
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      Load Balancer                           │
│                   (AWS ALB / K8s Ingress)                   │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
┌──────────────────┐            ┌──────────────────┐
│  API Gateway #1  │            │  API Gateway #2  │
│   (Port 8080)    │            │   (Port 8080)    │
└─────────┬────────┘            └─────────┬────────┘
          │                               │
          │    ┌──────────────────────────┤
          │    │                          │
          ▼    ▼                          ▼
     ┌─────────────┐              ┌─────────────┐
     │   Redis     │              │  Identity   │
     │  (Rate      │              │  Service    │
     │  Limiting)  │              │  :8081      │
     └─────────────┘              └─────────────┘
```

### Request Flow

```
1. Client → Gateway
   │ HTTPS Request: POST /api/v1/identity/auth/login
   │ Headers: Content-Type, User-Agent
   │ Body: { username, password }
   │
2. Gateway → CORS Check
   │ ✓ Origin allowed
   │ ✓ Headers allowed
   │
3. Gateway → Authentication Filter
   │ Skip (public endpoint: /auth/login)
   │
4. Gateway → Route Resolution
   │ Pattern: /api/v1/identity/*
   │ Target: http://identity-service:8081
   │
5. Gateway → Rate Limit Check
   │ Key: ratelimit:default:POST:/api/v1/identity/auth/login
   │ ✓ Within limit (15/100)
   │
6. Gateway → Proxy Request
   │ Forward to: http://identity-service:8081/api/v1/identity/auth/login
   │ Headers added: X-Trace-Id, X-Gateway-Timestamp
   │ Timeout: 5s
   │
7. Identity Service → Response
   │ Status: 200 OK
   │ Body: { token, expiresAt, user }
   │
8. Gateway → Response Transformation
   │ Add headers: X-RateLimit-*, X-Trace-Id
   │ Log request: status=200, duration=123ms
   │ Record metrics: gateway_requests_total++
   │
9. Gateway → Client
   │ Status: 200 OK
   │ Body: { token, expiresAt, user }
```

### Data Models

#### Route Configuration
```kotlin
data class ServiceRoute(
    val pattern: String,              // e.g., "/api/v1/identity/*"
    val target: ServiceTarget,
    val authRequired: Boolean = true,
    val rateLimit: RateLimitConfig?
)

data class ServiceTarget(
    val baseUrl: String,              // e.g., "http://identity-service:8081"
    val timeout: Duration = 5.seconds,
    val retries: Int = 2,
    val circuitBreaker: CircuitBreakerConfig? = null
)
```

#### Rate Limit Configuration
```kotlin
data class RateLimitConfig(
    val requestsPerWindow: Int,       // e.g., 100
    val windowDuration: Duration,     // e.g., 1.minutes
    val burstSize: Int = requestsPerWindow * 2
)

data class RateLimitResult(
    val allowed: Boolean,
    val remaining: Int,
    val resetAt: Instant
)
```

---

## Testing Strategy

### Unit Tests (Target: 85% coverage)

**Categories:**
1. **Route Resolution** (10 tests)
   - Pattern matching (exact, wildcard, regex)
   - Priority ordering
   - Default routes
   - Invalid patterns

2. **Authentication** (12 tests)
   - Valid JWT validation
   - Expired token rejection
   - Invalid signature rejection
   - Missing token handling
   - Claim extraction

3. **Rate Limiting** (8 tests)
   - Token bucket algorithm
   - Sliding window calculation
   - Redis key generation
   - Limit enforcement

4. **Error Handling** (10 tests)
   - Exception mapping
   - Error response format
   - Correlation ID inclusion

**Example Test:**
```kotlin
@QuarkusTest
class RouteResolverTest {
    
    @Inject
    lateinit var routeResolver: RouteResolver
    
    @Test
    fun `should resolve identity route correctly`() {
        val route = routeResolver.resolve("/api/v1/identity/users")
        
        assertThat(route).isNotNull
        assertThat(route.target.baseUrl).contains("identity-service")
        assertThat(route.authRequired).isTrue()
    }
    
    @Test
    fun `should throw RouteNotFoundException for unknown path`() {
        assertThrows<RouteNotFoundException> {
            routeResolver.resolve("/api/v1/unknown/resource")
        }
    }
}
```

### Integration Tests (Target: 15 tests)

**Categories:**
1. **End-to-End Request Flow** (5 tests)
   - Authenticated request to identity service
   - Public endpoint access
   - Rate limit exceeded scenario
   - Invalid JWT rejection
   - Service unavailable handling

2. **Redis Integration** (3 tests)
   - Rate limit persistence
   - Concurrent request handling
   - Redis failure fallback

3. **Service Communication** (4 tests)
   - Request forwarding with WireMock
   - Header propagation
   - Response streaming
   - Timeout handling

4. **CORS** (3 tests)
   - Preflight requests
   - Allowed origins
   - Blocked origins

**Example Test:**
```kotlin
@QuarkusTest
@TestProfile(IntegrationTestProfile::class)
class GatewayIntegrationTest {
    
    @Inject
    lateinit var mockServer: WireMockServer
    
    @Test
    fun `should forward authenticated request to identity service`() {
        // Setup mock identity service
        mockServer.stubFor(
            get(urlPathEqualTo("/api/v1/identity/users/me"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("""{"id":"user123","username":"admin"}""")
                )
        )
        
        // Make request to gateway with valid JWT
        given()
            .header("Authorization", "Bearer $validToken")
            .`when`()
            .get("/api/v1/identity/users/me")
            .then()
            .statusCode(200)
            .body("username", equalTo("admin"))
    }
}
```

### Load Tests (Target: 1000 req/s)

**Tool:** Gatling or K6

**Scenarios:**
1. **Baseline Load** (100 req/s for 5 minutes)
   - Verify p95 latency < 50ms
   - No errors

2. **Peak Load** (1000 req/s for 2 minutes)
   - Verify p95 latency < 100ms
   - Error rate < 0.1%

3. **Rate Limit Test**
   - Single tenant exceeds limit
   - Verify 429 responses
   - Verify other tenants unaffected

**Example K6 Script:**
```javascript
import http from 'k6/http';
import { check } from 'k6';

export let options = {
    stages: [
        { duration: '1m', target: 100 },  // Ramp up
        { duration: '5m', target: 1000 }, // Peak
        { duration: '1m', target: 0 },    // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<100'],
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const res = http.get('http://localhost:8080/api/v1/identity/health');
    check(res, {
        'status is 200': (r) => r.status === 200,
        'latency < 100ms': (r) => r.timings.duration < 100,
    });
}
```

---

## Risk Management

### High Risks

| Risk | Impact | Probability | Mitigation | Owner |
|------|--------|-------------|------------|-------|
| **Redis unavailable** | Rate limiting fails, gateway degrades | Medium | Implement fallback (local cache, allow all) | Dev 2 |
| **Identity service unavailable** | No route to forward requests | High | Circuit breaker, health checks, graceful degradation | Dev 1 |
| **JWT public key rotation** | Token validation fails unexpectedly | Medium | Cache public keys, refresh on 401, alert on key change | Dev 1 |
| **Performance bottleneck** | Gateway can't handle 1000 req/s | Medium | Load testing early, profiling, horizontal scaling | Both |
| **Security vulnerability** | JWT bypass, rate limit bypass | Low | Security review, penetration testing, dependency scanning | Both |

### Medium Risks

| Risk | Impact | Probability | Mitigation | Owner |
|------|--------|-------------|------------|-------|
| **Configuration complexity** | Hard to manage multi-environment config | Medium | Externalize config, document examples, validation | Dev 1 |
| **Testing gaps** | Production bugs due to untested scenarios | Medium | Comprehensive test plan, code review, QA testing | Both |
| **Deployment issues** | Gateway doesn't start in Kubernetes | Low | Local K8s testing (minikube), deployment runbook | Both |

### Dependencies & Blockers

| Dependency | Status | Blocker Risk | Mitigation |
|-----------|--------|--------------|------------|
| Identity service JWT endpoint | 🟡 In progress | Low | Coordinate with identity team, mock endpoint | Dev 1 |
| Redis availability in staging | ✅ Available | None | - |
| Load balancer configuration | 📋 Planned | Low | Work with DevOps, document requirements | Dev 1 |

---

## Daily Milestones

### Pre-Sprint (Before Day 1)
**Focus:** Dependency Preparation

**Goals:**
- [x] Complete Story 0.1: Dependency & Placeholder Cleanup (1 hour)
  - Version catalog updated with Quarkus extensions
  - Test dependencies added
  - Placeholder inventory completed

**Deliverables:**
- libs.versions.toml updated
- Build environment ready
- Architecture baseline documented

**Status:** ✅ Complete (2025-11-09)

---

### Day 1 (Mon, Nov 11)
**Focus:** Setup & Configuration

**Goals:**
- [ ] Sprint kickoff meeting (1 hour)
- [ ] Review ADR-004, DEVELOPER_ADVISORY (sections 1-7), ADR-006
- [ ] Complete Story 1.1: Project Setup (5 hours - adjusted)
- [ ] Start Story 1.2: Route Configuration (2 hours)

**Deliverables:**
- Gateway builds successfully with all dependencies
- Configuration aligns with .env.example
- Placeholder files replaced
- README documents security patterns

**EOD Status:** 35% Epic 1 complete

**Key Focus:** Security patterns from DEVELOPER_ADVISORY must be referenced in README

---

### Day 2 (Tue, Nov 12)
**Focus:** Routing Infrastructure

**Goals:**
- [ ] Complete Story 1.2: Route Configuration (6 hours)
- [ ] Start Story 1.3: Request Forwarding (2 hours)

**Deliverables:**
- Route resolution working
- Unit tests passing (>90% coverage)
- Service registry configured

**EOD Status:** 60% Epic 1 complete

---

### Day 3 (Wed, Nov 13)
**Focus:** Request Forwarding & Error Handling

**Goals:**
- [ ] Complete Story 1.3: Request Forwarding (10 hours)
- [ ] Start Story 1.4: Error Handling (2 hours)

**Deliverables:**
- HTTP client forwarding all methods
- Integration tests with WireMock
- Timeout and retry logic working

**EOD Status:** 85% Epic 1 complete

---

### Day 4 (Thu, Nov 14)
**Focus:** Error Handling & Authentication Setup

**Goals:**
- [ ] Complete Story 1.4: Error Handling (6 hours)
- [ ] Start Story 2.1: JWT Validation (4 hours)

**Deliverables:**
- Error responses standardized
- All exception types mapped
- JWT configuration started

**EOD Status:** Epic 1 complete, 35% Epic 2 complete

---

### Day 5 (Fri, Nov 15)
**Focus:** Authentication & Tenant Context

**Goals:**
- [ ] Complete Story 2.1: JWT Validation (8 hours)
- [ ] Complete Story 2.2: Tenant Context (4 hours)

**Deliverables:**
- JWT validation working
- Tenant context resolution
- Integration with identity service

**EOD Status:** 65% Epic 2 complete

---

### Day 6 (Mon, Nov 18)
**Focus:** Security Hardening + CORS & Public Endpoints

**Goals:**
- [ ] Complete Story 2.5: Security Hardening (3 hours)
- [ ] Complete Story 2.3: CORS (4 hours)
- [ ] Complete Story 2.4: Public Endpoints (2 hours)

**Deliverables:**
- Anti-enumeration patterns implemented
- Timing guards active
- ArchUnit tests passing
- CORS working
- Public endpoints accessible

**EOD Status:** Epic 2 complete, security baseline established

**Key Focus:** Security hardening from DEVELOPER_ADVISORY is critical for production readiness

---

### Day 7 (Tue, Nov 19)
**Focus:** Rate Limiting Setup

**Goals:**
- [ ] Complete Story 3.1: Redis Integration (4 hours)
- [ ] Start Story 3.2: Rate Limiter (4 hours)

**Deliverables:**
- Redis connected
- Health check reports Redis status
- Token bucket algorithm started

**EOD Status:** 50% Epic 3 complete

---

### Day 8 (Wed, Nov 20)
**Focus:** Rate Limiting Implementation

**Goals:**
- [ ] Complete Story 3.2: Rate Limiter (4 hours remaining)
- [ ] Complete Story 3.3: Rate Limit Filter (4 hours)

**Deliverables:**
- Token bucket algorithm working
- Redis-backed rate limiting
- Rate limit filter end-to-end
- Unit tests passing

**EOD Status:** Epic 3 complete

---

### Day 9 (Thu, Nov 21)
**Focus:** Observability

**Goals:**
- [ ] Complete Story 4.1: Structured Logging (3 hours)
- [ ] Complete Story 4.2: Metrics (3 hours)
- [ ] Complete Story 4.3: Distributed Tracing (2 hours)

**Deliverables:**
- JSON logging configured
- Prometheus metrics exposed
- Distributed tracing active
- All observability features working

**EOD Status:** Epic 4 complete

---

### Day 10 (Fri, Nov 22)
**Focus:** Testing & Validation

**Goals:**
- [ ] Load testing (3 hours)
- [ ] Integration test suite completion (3 hours)
- [ ] End-to-end validation with identity service (2 hours)

**Deliverables:**
- Load test results documented (1000 req/s target)
- Integration tests passing
- E2E flow validated

**EOD Status:** All features implemented and tested

---

### Day 11 (Mon, Nov 25) - Extended Day
**Focus:** Polish, Documentation & Sprint Closure

**Goals:**
- [ ] Code review and fixes (3 hours)
- [ ] Documentation updates:
  - API documentation
  - Deployment runbook
  - Operations guide
  - README security patterns
- [ ] Security scan review (1 hour)
- [ ] Sprint demo preparation (1 hour)
- [ ] Sprint retrospective (1 hour)

**Note:** Extended to Day 11 due to Story 2.5 addition (security hardening)

**Deliverables:**
- All P0 stories complete
- Documentation up-to-date
- Sprint demo ready
- Retrospective insights captured

**EOD Status:** Sprint complete, ready for deployment

---

## Definition of Done

### Story-Level DoD

A story is considered done when:
- [ ] Code implemented according to acceptance criteria
- [ ] Unit tests written and passing (>85% coverage)
- [ ] Integration tests written and passing (where applicable)
- [ ] Code reviewed and approved by peer
- [ ] ktlint passing (no style violations)
- [ ] No new SonarQube critical/blocker issues
- [ ] Documentation updated (README, inline comments)
- [ ] CI/CD pipeline green

### Sprint-Level DoD

The sprint is considered done when:
- [ ] All P0 stories complete
- [ ] All tests passing (unit, integration, E2E)
- [ ] Code coverage > 80% overall
- [ ] Load test completed (1000 req/s achieved)
- [ ] Security scan clean (no HIGH/CRITICAL CVEs)
- [ ] Documentation complete:
  - [ ] API documentation
  - [ ] Deployment runbook
  - [ ] Operations guide
  - [ ] README updated
- [ ] Demo prepared and delivered
- [ ] Deployed to staging environment
- [ ] Sprint retrospective completed

---

## Post-Sprint Activities

### Sprint Review (Day 10, 2:00 PM)
**Duration:** 1 hour  
**Attendees:** Dev Team, Product Owner, Stakeholders

**Agenda:**
1. Sprint goal review (5 min)
2. Live demo of API Gateway (30 min):
   - Request routing
   - Authentication flow
   - Rate limiting demo
   - Metrics dashboard
3. Metrics review (10 min):
   - Velocity
   - Test coverage
   - Performance benchmarks
4. Feedback & questions (15 min)

### Sprint Retrospective (Day 10, 3:30 PM)
**Duration:** 1 hour  
**Attendees:** Dev Team

**Agenda:**
1. What went well? (15 min)
2. What could be improved? (15 min)
3. Action items for next sprint (15 min)
4. Team health check (15 min)

### Deployment to Staging (Day 10, EOD)
**Checklist:**
- [ ] Build Docker image
- [ ] Push to container registry
- [ ] Update Kubernetes manifests
- [ ] Deploy to staging cluster
- [ ] Run smoke tests
- [ ] Monitor metrics for 1 hour
- [ ] Document any issues

---

## Appendix

### Environment Variables

```bash
# api-gateway/config/api-gateway.env

# Server Configuration
QUARKUS_HTTP_PORT=8080
QUARKUS_HTTP_HOST=0.0.0.0

# JWT Configuration
JWT_PUBLIC_KEY_URL=http://identity-service:8081/api/v1/identity/.well-known/jwks.json
JWT_ISSUER=erp-platform

# Service URLs
IDENTITY_SERVICE_URL=http://identity-service:8081

# Redis Configuration
REDIS_URL=redis://redis:6379
REDIS_PASSWORD=

# Rate Limiting
RATE_LIMIT_DEFAULT_REQUESTS=100
RATE_LIMIT_DEFAULT_WINDOW=60s

# CORS Configuration
CORS_ORIGINS=http://localhost:3000,https://app.example.com

# Observability
QUARKUS_LOG_LEVEL=INFO
QUARKUS_LOG_CONSOLE_JSON=true
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317

# Performance
QUARKUS_THREAD_POOL_MAX_THREADS=200
QUARKUS_HTTP_LIMITS_MAX_BODY_SIZE=10M
```

### Key Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // Quarkus Core
    implementation("io.quarkus:quarkus-rest-client")
    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
    
    // Security
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")
    
    // Redis
    implementation("io.quarkus:quarkus-redis-client")
    
    // Observability
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-opentelemetry")
    implementation("io.quarkus:quarkus-logging-json")
    
    // Testing
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("com.github.tomakehurst:wiremock-jre8")
}
```

### Reference Documentation

- [Quarkus REST Client](https://quarkus.io/guides/rest-client)
- [Quarkus JWT](https://quarkus.io/guides/security-jwt)
- [Quarkus Redis](https://quarkus.io/guides/redis)
- [Quarkus Micrometer](https://quarkus.io/guides/micrometer)
- [Quarkus OpenTelemetry](https://quarkus.io/guides/opentelemetry)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)

---

**Next Steps After Sprint 3:**
1. Sprint 4: Expand gateway routing to additional contexts (commerce, inventory)
2. Sprint 5: API versioning and backward compatibility
3. Sprint 6: GraphQL gateway implementation
4. Sprint 7: API composition and aggregation patterns

**Sprint Sign-Off:**
- [ ] Sprint Planning Complete
- [ ] Team Commitment Confirmed
- [ ] Dependencies Identified
- [ ] Risks Documented
- [ ] Ready to Start
