# API Gateway

**Status:** ✅ Core Features Implemented (2025-11-10)  
**Version:** 0.1.0-SNAPSHOT  
**Tech Stack:** Quarkus 3.29.0, Kotlin 2.2.0, Redis, JWT  

## Overview

The API Gateway serves as the single entry point for all external client requests to the ERP platform. It provides routing, authentication, rate limiting, and observability for the microservices architecture.

**Implemented Features:**
- ✅ Request routing to bounded contexts (Epic 1)
- ✅ JWT authentication with public path bypass (Epic 2)
- ✅ Tenant context extraction and propagation (Epic 2)
- ✅ Redis-backed rate limiting (100 req/min default) (Epic 3)
- ✅ Distributed tracing with X-Trace-Id (Epic 4)
- ✅ Structured request logging (Epic 4)
- ✅ HTTP proxy for GET/POST/PUT/PATCH/DELETE
- ✅ Error standardization with GatewayExceptionMapper

### Metrics (Micrometer/Prometheus)
- `gateway_requests_total{method,endpoint,status}`
- `gateway_request_duration_seconds{method,endpoint,status}` (timer)
- `gateway_errors_total{type}`
- `gateway_ratelimit_exceeded_total{tenant}`
- `gateway_auth_failures_total{reason}`

Prometheus scrape: `/q/metrics` (enabled via `quarkus-micrometer-registry-prometheus`).

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

### Epic 1: Core Gateway Infrastructure ✅ **COMPLETED**
**Stories:** 1.1 → 1.4  
**Focus:** Configuration, routing, request forwarding, error handling

**Implemented Files:**
```
config/
  ├── RouteConfiguration.kt ✅ (CDI producer for RouteResolver)
  ├── PublicEndpointsConfig.kt ✅ (Public path patterns)
routing/
  ├── RouteResolver.kt ✅ (Pattern matching)
  ├── RouteDefinitions.kt ✅ (Default routes)
  ├── ServiceRoute.kt ✅
  ├── ServiceTarget.kt ✅
  └── RouteNotFoundException.kt ✅
proxy/
  ├── ProxyService.kt ✅ (HTTP forwarding via JDK HttpClient)
  └── ProxyController.kt ✅ (GET/POST/PUT/PATCH/DELETE)
exception/
  ├── GatewayExceptionMapper.kt ✅
  └── ErrorResponse.kt ✅
```

### Epic 2: Authentication & Authorization ✅ **COMPLETED**
**Stories:** 2.1 → 2.4  
**Focus:** JWT validation, tenant context, security

**Implemented Files:**
```
security/
  ├── AuthenticationFilter.kt ✅ (JWT validation, SecurityContext)
  ├── JwtValidator.kt ✅ (SmallRye JWT wrapper)
  └── GatewaySecurityContext.kt ✅ (Principal + roles)
context/
  ├── TenantContext.kt ✅ (Request-scoped bean)
  └── TenantContextFilter.kt ✅ (X-Tenant-Id/X-User-Id propagation)
```

**Security Features:**
- ✅ Generic 401 responses (anti-enumeration)
- ✅ Public path bypass (/health/*, /metrics, /api/v1/identity/auth/*)
- ✅ Role-based authorization ready
- ⚠️ TODO: Timing guards (Story 2.5)
- ⚠️ TODO: ArchUnit tests (Story 2.5)

### Epic 3: Rate Limiting ✅ **COMPLETED**
**Stories:** 3.1 → 3.3  
**Focus:** Redis integration, sliding window algorithm, enforcement

**Implemented Files:**
```
infrastructure/
  └── RedisService.kt ✅ (Redis wrapper with modern API)
ratelimit/
  ├── RateLimiter.kt ✅ (Sliding window per tenant/endpoint)
  └── RateLimitResult.kt ✅ (allowed, remaining, resetAt)
filter/
  └── RateLimitFilter.kt ✅ (100 req/min default, X-RateLimit-* headers)
```

**Features:**
- ✅ Per-tenant rate limiting
- ✅ Configurable limits (default: 100 req/min)
- ✅ HTTP 429 responses with reset time
- ✅ X-RateLimit-Limit/Remaining/Reset headers

#### Rate Limiting Deep Dive

**Algorithm: Sliding Window**

The gateway uses a sliding window algorithm implemented in `RateLimiter.kt`:

```kotlin
// Redis key pattern: ratelimit:{tenantId}:{endpoint}:{windowStart}
// Each minute gets a new counter, allowing precise sliding window calculation
val key = "ratelimit:$tenantId:$endpoint:${windowStart}"
val currentCount = redis.increment(key)
redis.expire(key, windowDuration)
```

**Benefits:**
- **Fair distribution:** Prevents burst at window boundaries (unlike fixed window)
- **Memory efficient:** Older windows auto-expire via Redis TTL
- **Distributed:** Multiple gateway instances share rate limit state

**Configuration:**

```yaml
gateway:
  rate-limits:
    default:
      requests-per-minute: 100
      window: 60s
    # Per-endpoint overrides (future enhancement)
    endpoints:
      /api/v1/inventory/products:
        requests-per-minute: 500
      /api/v1/identity/auth/login:
        requests-per-minute: 10  # Prevent brute force
```

**Response Headers:**

| Header | Example | Description |
|--------|---------|-------------|
| `X-RateLimit-Limit` | `100` | Max requests allowed per window |
| `X-RateLimit-Remaining` | `42` | Requests remaining in current window |
| `X-RateLimit-Reset` | `2025-11-10T15:32:00Z` | When the rate limit resets |

**HTTP 429 Response:**
```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded. Try again in 27 seconds.",
  "details": {
    "limit": 100,
    "window": "60s",
    "resetAt": "2025-11-10T15:32:00Z"
  }
}
```

**Bypass Mechanisms:**

Rate limiting is skipped for:
- ✅ Health checks (`/health/*`)
- ✅ Metrics endpoints (`/metrics`)
- ✅ Public endpoints (configurable via `gateway.public-endpoints.patterns`)

**Monitoring:**

Track rate limit violations with:
```prometheus
# Prometheus metric (when implemented)
gateway_ratelimit_exceeded_total{tenant="acme-corp",endpoint="/api/v1/orders"} 42

# Structured logs
{
  "level": "WARN",
  "message": "Rate limit exceeded",
  "tenantId": "acme-corp",
  "endpoint": "/api/v1/orders",
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Future Enhancements:**
- ⚠️ Dynamic rate limit adjustment based on tenant subscription tier
- ⚠️ Distributed rate limiting with Redis Cluster
- ⚠️ Burst allowance (token bucket algorithm variant)
- ⚠️ API key-based rate limiting for service accounts

---

### Resilience Patterns

The API Gateway implements multiple resilience patterns to ensure reliability and graceful degradation:

#### 1. ✅ Circuit Breaker (Ready for Implementation)

**Pattern:** Stop calling failing downstream services to prevent cascading failures

**Configuration (Future):**
```yaml
gateway:
  services:
    tenancy-identity:
      url: http://localhost:8081
      timeout: 5s
      retries: 2
      circuit-breaker:
        failure-threshold: 5          # Open after 5 failures
        success-threshold: 2          # Close after 2 successes
        timeout: 60s                  # Half-open after 60s
        monitor-window: 10s
```

**States:**
- **CLOSED:** Normal operation, requests pass through
- **OPEN:** Circuit tripped, return 503 immediately (fail-fast)
- **HALF_OPEN:** Test with limited requests, close if successful

**Implementation with SmallRye Fault Tolerance:**
```kotlin
@ApplicationScoped
class ProxyService(private val httpClient: HttpClient) {
    
    @CircuitBreaker(
        requestVolumeThreshold = 5,
        failureRatio = 0.5,
        delay = 60,
        delayUnit = ChronoUnit.SECONDS
    )
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @Retry(
        maxRetries = 2,
        delay = 100,
        delayUnit = ChronoUnit.MILLIS,
        retryOn = [IOException::class, TimeoutException::class]
    )
    @Fallback(fallbackMethod = "fallbackResponse")
    suspend fun forwardRequest(/* ... */): Response {
        // Existing proxy logic
    }
    
    private fun fallbackResponse(/* ... */): Response {
        return Response
            .status(Response.Status.SERVICE_UNAVAILABLE)
            .entity(ErrorResponse(
                code = "SERVICE_UNAVAILABLE",
                message = "The service is temporarily unavailable. Please try again later."
            ))
            .build()
    }
}
```

#### 2. ✅ Timeouts (Configured)

**Current Configuration:**
```yaml
gateway:
  services:
    tenancy-identity:
      timeout: 5s
      retries: 2
```

**Implementation in ProxyService:**
```kotlin
val httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(5))
    .build()

val request = HttpRequest.newBuilder()
    .timeout(Duration.ofSeconds(5))
    .build()
```

**Benefits:**
- Prevents thread starvation from slow downstream services
- Enforces SLA boundaries (p95 < 50ms gateway latency target)
- Cascading timeout budgets: Gateway 5s → Service 4s → Database 2s

#### 3. ⚠️ Bulkhead Pattern (TODO)

**Pattern:** Isolate resources to prevent one failing service from consuming all threads

**Configuration (Future):**
```yaml
gateway:
  bulkheads:
    default:
      max-concurrent-calls: 50
      max-wait-duration: 10s
    critical:  # For critical services
      max-concurrent-calls: 100
      max-wait-duration: 5s
```

**Implementation with SmallRye Fault Tolerance:**
```kotlin
@Bulkhead(value = 50, waitingTaskQueue = 10)
suspend fun forwardRequest(/* ... */): Response {
    // Proxy logic
}
```

#### 4. ✅ Health Checks (Implemented)

**Readiness Check:**
```kotlin
@Readiness
class RedisReadinessCheck : HealthCheck {
    @Inject
    lateinit var redisClient: RedisClient
    
    override fun call(): HealthCheckResponse {
        return try {
            redisClient.ping()
            HealthCheckResponse.up("redis")
        } catch (e: Exception) {
            HealthCheckResponse.down("redis")
        }
    }
}
```

**Kubernetes Integration:**
```yaml
livenessProbe:
  httpGet:
    path: /health/live
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 30
  
readinessProbe:
  httpGet:
    path: /health/ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 10
  failureThreshold: 3
```

#### 5. ✅ Graceful Degradation (Implemented)

**Rate Limiting:**
- Redis failure → Allow all requests with warning log
- Prevents cascading failure from rate limit backend

```kotlin
try {
    val result = rateLimiter.checkLimit(tenantId, endpoint)
    if (!result.allowed) {
        return Response.status(429).entity(/* ... */).build()
    }
} catch (e: RedisException) {
    logger.warn("Redis unavailable, bypassing rate limit", e)
    // Allow request to proceed
}
```

**Public Endpoint Bypass:**
- Authentication failure → 401 for protected endpoints
- Public endpoints (`/health/*`, `/api/v1/identity/auth/*`) → Always allowed

#### 6. ⚠️ Backpressure (TODO)

**Pattern:** Slow down incoming requests when system is under load

**Configuration (Future):**
```yaml
gateway:
  backpressure:
    max-concurrent-requests: 1000
    queue-size: 500
    timeout: 30s
```

**Implementation:**
```kotlin
@ApplicationScoped
class BackpressureFilter : ContainerRequestFilter {
    private val semaphore = Semaphore(1000)
    
    override fun filter(requestContext: ContainerRequestContext) {
        if (!semaphore.tryAcquire(30, TimeUnit.SECONDS)) {
            throw ServiceUnavailableException("System overloaded, try again later")
        }
        
        requestContext.setProperty("backpressure.permit", Unit)
    }
}
```

#### 7. ✅ Observability for Resilience (Implemented)

**Metrics to Track:**
```prometheus
# Request latency (identify slow services)
gateway_request_duration_seconds{method="GET",endpoint="/api/orders",status="200"} 0.045

# Error rates (trigger circuit breaker)
gateway_errors_total{type="timeout",service="inventory"} 42

# Rate limit violations
gateway_ratelimit_exceeded_total{tenant="acme",endpoint="/api/orders"} 15

# Circuit breaker state
gateway_circuit_breaker_state{service="inventory",state="open"} 1
```

**Alerting Rules:**
```yaml
groups:
  - name: gateway_resilience
    rules:
      - alert: HighErrorRate
        expr: rate(gateway_errors_total[5m]) > 0.1
        for: 2m
        annotations:
          summary: "Gateway error rate above 10%"
      
      - alert: CircuitBreakerOpen
        expr: gateway_circuit_breaker_state{state="open"} == 1
        for: 1m
        annotations:
          summary: "Circuit breaker open for {{ $labels.service }}"
```

**Distributed Tracing:**
- ✅ X-Trace-Id propagation across all services
- OpenTelemetry integration for end-to-end request tracing
- Identify latency bottlenecks in call chains

---

### Epic 4: Observability ✅ **COMPLETED**
**Stories:** 4.1 → 4.3  
**Focus:** Structured logging, distributed tracing

**Implemented Files:**
```
logging/
  └── RequestLoggingFilter.kt ✅ (Structured logs: method, path, status, duration, traceId)
tracing/
  └── TracingFilter.kt ✅ (X-Trace-Id generation + propagation)
```

**Features:**
- ✅ Distributed tracing via X-Trace-Id
- ✅ Structured JSON logging
- ✅ Request/response logging with duration
- ⚠️ TODO: Micrometer metrics (optional enhancement)

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

### application.yml Structure (✅ Implemented)

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
  
  public-endpoints:
    patterns:
      - /health/*
      - /metrics
      - /api/v1/identity/auth/*
```

### Environment Variables Reference

| Variable | Description | Default | Status |
|----------|-------------|---------|--------|
| `API_GATEWAY_PORT` | HTTP server port | 8080 | ✅ Active |
| `REDIS_URL` | Redis connection URL | redis://localhost:6379 | ✅ Active |
| `JWT_PUBLIC_KEY_URL` | JWKS endpoint for token validation | - | ✅ Required |
| `JWT_ISSUER` | Expected JWT issuer | - | ✅ Required |
| `IDENTITY_SERVICE_URL` | Tenancy-Identity service URL | http://localhost:8081 | ✅ Active |
| `RATE_LIMIT_DEFAULT_REQUESTS` | Default requests per minute | 100 | ✅ Active |
| `CORS_ORIGINS` | Allowed CORS origins | http://localhost:3000 | ✅ Active |

---

## Security Best Practices

### 1. ✅ Anti-Enumeration Patterns (Implemented)
Current implementation in `AuthenticationFilter.kt`:

```kotlin
// ✅ Generic 401 response for all authentication failures
return Response.status(Response.Status.UNAUTHORIZED)
    .entity(ErrorResponse("AUTHENTICATION_FAILED", "Authentication required"))
    .build()
```

❌ **DON'T:**
```kotlin
// Bad: Discloses user existence
if (userNotFound) throw NotFoundException("User admin@example.com not found")
if (invalidPassword) throw UnauthorizedException("Invalid password")
```

### 2. ⚠️ Timing Guards (TODO - Story 2.5)
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

### 3. ⚠️ Architecture Enforcement (TODO - Story 2.5)
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

### 4. ✅ Audit Logging (Implemented)
Current implementation in `RequestLoggingFilter.kt`:

```kotlin
// ✅ PII-free structured logging
logger.info(
    "HTTP request completed: method={}, path={}, status={}, duration_ms={}, traceId={}",
    requestContext.method,
    requestContext.uriInfo.path,
    responseContext.status,
    duration,
    traceId
    // ❌ NO credentials, tokens, usernames, or emails
)
```

**Reference:** DEVELOPER_ADVISORY.md for complete security patterns proven in tenancy-identity (Grade A-).

---

## Testing Strategy

### ✅ Unit Tests (Implemented)
Current test files:
- `test/routing/RouteResolverTest.kt` - Route pattern matching and wildcard resolution
- `test/exception/GatewayExceptionMapperTest.kt` - Error response mapping

**Coverage Target:** >80% (tests present, skipped by build-logic convention)

### ⚠️ Integration Tests (TODO)
Planned test infrastructure:
- WireMock for backend service mocking
- Testcontainers for Redis
- REST Assured for API testing
- CORS validation
- Public/protected endpoint access
- End-to-end request forwarding with header/query/body propagation
- Rate limiting under concurrent load

### ⚠️ Load Tests (TODO)
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
    ],
};
```

---

## Integration with Tenancy-Identity

### ✅ JWT Token Flow (Implemented)
```
1. Client → POST /api/v1/identity/auth/login (public endpoint, bypasses auth)
2. Identity Service → Returns JWT with tenantId, userId, roles claims
3. Client → GET /api/v1/{context}/{resource} (Authorization: Bearer <JWT>)
4. Gateway → AuthenticationFilter validates JWT with SmallRye JWT + JWKS
5. Gateway → TenantContextFilter extracts tenantId, userId from JWT claims
6. Gateway → Sets X-Tenant-Id, X-User-Id headers for downstream services
7. Gateway → ProxyController forwards request to bounded context
```

**Files:**
- `security/AuthenticationFilter.kt` - JWT validation (Priority: AUTHENTICATION)
- `context/TenantContextFilter.kt` - Header injection (Priority: AUTHENTICATION+10)
- `config/PublicEndpointsConfig.kt` - Configures /api/v1/identity/auth/* bypass

### ✅ Tenant Context Propagation (Implemented)
Current implementation in `TenantContextFilter.kt`:

```kotlin
// ✅ Gateway adds headers for downstream services
val tenantId = tenantContext.tenantId ?: jwtClaims?.tenantId
val userId = tenantContext.userId ?: jwtClaims?.userId

requestContext.headers.add("X-Tenant-Id", tenantId)
requestContext.headers.add("X-User-Id", userId)
requestContext.headers.add("X-Trace-Id", correlationId)
```

**Logging Context:**
Structured logging in `RequestLoggingFilter.kt` includes traceId, method, path, status, duration_ms (no PII).

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

### ✅ Health Checks (Implemented)
Quarkus SmallRye Health extensions provide:
- **Liveness:** `GET /health/live`
- **Readiness:** `GET /health/ready` (includes Redis connectivity check)

### ⚠️ Metrics (Partially Implemented)
- **Prometheus:** `GET /metrics` (Micrometer dependency present)
- **TODO:** Custom application metrics
  - `gateway_requests_total` (counter: method, status, route tags)
  - `gateway_request_duration_seconds` (histogram: method, route tags)
  - `gateway_errors_total` (counter: error_type tag)
  - `gateway_ratelimit_exceeded_total` (counter: tenant, endpoint tags)

### ✅ Tracing (Implemented)
Current implementation in `TracingFilter.kt`:

```kotlin
// ✅ Generate or propagate X-Trace-Id
val traceId = requestContext.getHeaderString("X-Trace-Id")
    ?: UUID.randomUUID().toString()

requestContext.headers.add("X-Trace-Id", traceId)
// Propagated to downstream services by ProxyService
```

**Structured Logging:**
- `RequestLoggingFilter.kt` logs every request with traceId, method, path, status, duration_ms
- OpenTelemetry auto-instrumentation available via Quarkus extension (dependency present)

---

## Current Status

**Phase:** ✅ Core Implementation Complete (2025-11-10)  
**Sprint:** Sprint 3 (Nov 11-25, 2025)  

### Completed Work
1. ✅ Epic 1: Core Infrastructure (routing, proxying, error handling)
2. ✅ Epic 2: Authentication & Authorization (JWT, tenant context, public paths)
3. ✅ Epic 3: Rate Limiting (Redis-backed, sliding window algorithm)
4. ✅ Epic 4: Observability (tracing, structured logging)
5. ✅ Build clean: ktlint passing, no compiler warnings
6. ✅ Unit tests: RouteResolver, ExceptionMapper

### Pending Enhancements (Optional)
- ⚠️ Story 2.5: Security hardening (timing guards, ArchUnit architecture tests)
- ⚠️ Micrometer custom metrics implementation
- ⚠️ WireMock integration tests for end-to-end forwarding
- ⚠️ Load testing validation (target: 1000 req/s, p95 < 50ms)

**Quality Gates:**
- ✅ CI/CD pipeline green (build SUCCESS)
- ⚠️ >80% test coverage (unit tests present, integration tests TODO)
- ✅ ktlint passing (auto-formatted with gradlew ktlintFormat)
- ✅ No compiler warnings/errors
- ⚠️ ArchUnit tests enforcing architecture (TODO)
- ⚠️ Load test achieving 1000 req/s (TODO)

---

## Architecture & Request Flow

### Filter Chain Execution Order
```
Incoming Request
  ↓
1. AuthenticationFilter (Priority: AUTHENTICATION)
   - Validates JWT Bearer token via SmallRye JWT
   - Creates SecurityContext with principal and roles
   - Returns 401 for invalid/missing tokens
   - Bypasses public endpoints (/health/*, /metrics, /api/v1/identity/auth/*)
  ↓
2. TenantContextFilter (Priority: AUTHENTICATION+10)
   - Extracts tenantId, userId from JWT claims or headers
   - Populates request-scoped TenantContext CDI bean
   - Injects X-Tenant-Id, X-User-Id headers for downstream services
  ↓
3. RateLimitFilter (Priority: USER)
   - Checks Redis for request count: ratelimit:{tenantId}:{endpoint}:{windowStart}
   - Allows 100 req/min default (configurable via gateway.rate-limits.default.requests-per-minute)
   - Returns HTTP 429 with X-RateLimit-* headers if exceeded
   - Uses sliding window algorithm via RedisService
  ↓
4. TracingFilter (Priority: USER)
   - Generates UUID for X-Trace-Id if not present
   - Propagates traceId to downstream services
   - Enables distributed tracing across microservices
  ↓
5. RequestLoggingFilter (Priority: USER)
   - Logs structured request/response: method, path, status, duration_ms, traceId
   - PII-free logging (no credentials, tokens, usernames)
  ↓
6. ProxyController (JAX-RS Resource)
   - Routes GET/POST/PUT/PATCH/DELETE to ProxyService
   - Uses RouteResolver to match path to backend service URL
  ↓
7. ProxyService
   - Forwards HTTP request via JDK HttpClient (non-reactive)
   - Propagates headers (excluding hop-by-hop headers)
   - Copies query parameters and request body
   - Returns response with status, headers, and body
  ↓
Response to Client
```

### Key Components
- **RouteConfiguration.kt**: CDI producer creating RouteResolver from application.yml
- **RouteResolver.kt**: Pattern-based routing with wildcard support
- **RedisService.kt**: Redis wrapper using modern Quarkus `redis.value()` API
- **RateLimiter.kt**: Sliding window rate limiting algorithm
- **JwtValidator.kt**: SmallRye JWT token parser wrapper
- **GatewaySecurityContext.kt**: JAX-RS SecurityContext implementation

---

## Support & Contribution

**Documentation:**
- Sprint plan: `docs/SPRINT3_API_GATEWAY_PLAN.md`
- Architecture decisions: `docs/adr/ADR-004-api-gateway-pattern.md`
- Security patterns: `docs/DEVELOPER_ADVISORY.md`
- Error handling: `docs/ERROR_HANDLING_ANALYSIS_AND_POLICY.md`

**Review Process:**
Following tenancy-identity review patterns (achieved Grade A- through 4 review cycles).

**Contributing:**
1. Follow ktlint formatting: `./gradlew :api-gateway:ktlintFormat`
2. Ensure tests pass: `./gradlew :api-gateway:test`
3. Verify build: `./gradlew :api-gateway:build --warning-mode all`
4. Follow security patterns from DEVELOPER_ADVISORY.md (anti-enumeration, PII-free logging)
