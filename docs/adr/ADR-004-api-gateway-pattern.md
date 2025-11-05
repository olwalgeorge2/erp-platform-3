# ADR-004: API Gateway Pattern

**Status**: Accepted  
**Date**: 2025-11-05  
**Deciders**: Architecture Team, Platform Team  
**Tags**: api-gateway, security, routing, cross-cutting

## Context

Our microservices architecture exposes multiple bounded contexts, each with their own APIs. External clients (web, mobile, third-party integrations) need a consistent entry point with unified authentication, rate limiting, and routing logic.

## Decision

We will implement a **centralized API Gateway** using Quarkus as the single entry point for all external API traffic.

### API Gateway Responsibilities

1. **Request Routing**
   - Route requests to appropriate bounded context services
   - URL pattern matching: `/api/v1/{context}/{resource}`
   - Dynamic service discovery (Kubernetes services)
   - Load balancing across service instances

2. **Authentication & Authorization**
   - JWT token validation
   - Integration with `tenancy-identity` context for auth
   - Tenant resolution from token
   - RBAC enforcement at gateway level

3. **Rate Limiting**
   - Per-tenant rate limits
   - Per-endpoint rate limits
   - Configured via `config/rate-limits.yml`
   - Redis-backed distributed rate limiting

4. **Cross-Cutting Concerns**
   - Request/response logging
   - Distributed tracing (correlation IDs)
   - Metrics collection
   - Error standardization
   - CORS handling

5. **API Composition (Limited)**
   - Simple aggregation for specific use cases
   - Backend-for-Frontend (BFF) patterns
   - Avoid complex orchestration (use events instead)

## Rationale

### Why a Gateway?
- ✅ **Single Entry Point**: Simplified client configuration
- ✅ **Security**: Centralized authentication/authorization
- ✅ **Cross-Cutting Concerns**: DRY for logging, tracing, etc.
- ✅ **Versioning**: API version management in one place
- ✅ **Protocol Translation**: REST → gRPC if needed
- ✅ **Rate Limiting**: Protect backend services

### Why Quarkus Gateway (Not Kong, NGINX, etc.)?
- ✅ Same tech stack as services (Kotlin/Quarkus)
- ✅ Easy integration with our CQRS/event infrastructure
- ✅ Type-safe configuration
- ✅ Fast startup (important for scaling)
- ✅ Native image support for production
- ✅ Full control over logic

## Consequences

### Positive
- ✅ Clients deal with single endpoint
- ✅ Backend services shielded from external traffic
- ✅ Centralized security enforcement
- ✅ Easy to add caching, compression, etc.
- ✅ Simplified monitoring (all traffic flows through one point)
- ✅ Can version APIs independently of services

### Negative
- ❌ Single point of failure (mitigated by HA deployment)
- ❌ Potential bottleneck (mitigated by horizontal scaling)
- ❌ Added latency (minimal with Quarkus)
- ❌ Another service to maintain
- ❌ Risk of becoming a "god service" with too much logic

### Neutral
- ⚖️ Need load balancer in front of gateway
- ⚖️ Requires Redis for distributed rate limiting
- ⚖️ Monitoring must be comprehensive

## Implementation Details

### URL Routing Pattern

```
https://api.example.com/api/v1/{context}/{resource}

Examples:
/api/v1/commerce/orders
/api/v1/financial/accounts
/api/v1/inventory/products
/api/v1/customers/contacts
```

### Gateway Configuration

```kotlin
// api-gateway/src/main/kotlin/config/RouteConfiguration.kt
@ApplicationScoped
class RouteConfiguration {
    
    @ConfigProperty(name = "gateway.services.commerce.url")
    lateinit var commerceServiceUrl: String
    
    @ConfigProperty(name = "gateway.services.financial.url")
    lateinit var financialServiceUrl: String
    
    fun routes(): List<Route> = listOf(
        Route("/api/v1/commerce/*", commerceServiceUrl),
        Route("/api/v1/financial/*", financialServiceUrl),
        Route("/api/v1/inventory/*", inventoryServiceUrl),
        // ... other routes
    )
}
```

### Authentication Filter

```kotlin
@Provider
@Priority(Priorities.AUTHENTICATION)
class AuthenticationFilter(
    private val jwtValidator: JwtValidator,
    private val tenantContext: TenantContext
) : ContainerRequestFilter {
    
    override fun filter(requestContext: ContainerRequestContext) {
        val token = extractToken(requestContext)
            ?: throw UnauthorizedException("Missing authentication token")
        
        val claims = jwtValidator.validate(token)
        
        // Set tenant context
        tenantContext.setTenantId(claims.tenantId)
        tenantContext.setUserId(claims.userId)
        tenantContext.setRoles(claims.roles)
    }
}
```

### Rate Limiting

```yaml
# config/rate-limits.yml
rate-limits:
  default:
    requests-per-minute: 100
    burst: 20
  
  per-endpoint:
    - path: "/api/v1/commerce/orders"
      requests-per-minute: 50
    - path: "/api/v1/financial/accounts"
      requests-per-minute: 30
  
  per-tenant:
    - tier: "free"
      requests-per-minute: 50
    - tier: "premium"
      requests-per-minute: 500
    - tier: "enterprise"
      requests-per-minute: 5000
```

```kotlin
@ApplicationScoped
class RateLimiter(
    private val redisClient: RedisClient,
    private val rateLimitConfig: RateLimitConfig
) {
    
    fun checkRateLimit(tenantId: TenantId, endpoint: String): Boolean {
        val key = "ratelimit:${tenantId}:${endpoint}"
        val limit = rateLimitConfig.getLimit(tenantId, endpoint)
        
        val current = redisClient.incr(key)
        if (current == 1L) {
            redisClient.expire(key, 60) // 60 seconds
        }
        
        return current <= limit
    }
}
```

### Distributed Tracing

```kotlin
@Provider
class TracingFilter(
    private val tracer: Tracer
) : ContainerRequestFilter {
    
    override fun filter(requestContext: ContainerRequestContext) {
        val correlationId = requestContext.getHeaderString("X-Correlation-ID")
            ?: UUID.randomUUID().toString()
        
        // Create span
        val span = tracer.spanBuilder("api-gateway-request")
            .setAttribute("http.method", requestContext.method)
            .setAttribute("http.path", requestContext.uriInfo.path)
            .setAttribute("correlation.id", correlationId)
            .setAttribute("tenant.id", tenantContext.getTenantId())
            .startSpan()
        
        requestContext.setProperty("trace-span", span)
        requestContext.headers.putSingle("X-Correlation-ID", correlationId)
    }
}
```

### Error Handling

```kotlin
@Provider
class GlobalExceptionMapper : ExceptionMapper<Exception> {
    
    override fun toResponse(exception: Exception): Response {
        val errorResponse = when (exception) {
            is UnauthorizedException -> ErrorResponse(
                code = "UNAUTHORIZED",
                message = exception.message,
                status = 401
            )
            is RateLimitExceededException -> ErrorResponse(
                code = "RATE_LIMIT_EXCEEDED",
                message = "Too many requests",
                status = 429
            )
            is NotFoundException -> ErrorResponse(
                code = "NOT_FOUND",
                message = exception.message,
                status = 404
            )
            else -> ErrorResponse(
                code = "INTERNAL_ERROR",
                message = "An unexpected error occurred",
                status = 500
            )
        }
        
        return Response
            .status(errorResponse.status)
            .entity(errorResponse)
            .build()
    }
}
```

## Service Discovery

### Kubernetes-based Discovery
```yaml
# Kubernetes service names match context names
apiVersion: v1
kind: Service
metadata:
  name: commerce-service
spec:
  selector:
    app: commerce-ecommerce
  ports:
    - port: 8080
```

```kotlin
// Gateway resolves: http://commerce-service:8080
@ConfigProperty(name = "gateway.services.commerce.url")
val commerceUrl = "http://commerce-service:8080"
```

## API Versioning Strategy

### URL-based Versioning (Chosen)
```
/api/v1/commerce/orders
/api/v2/commerce/orders  // New version
```

**Rationale**: Simple, explicit, easy to route, cacheable

### Header-based Versioning (Considered)
```
Accept: application/vnd.erp.v1+json
```
**Rejected**: More complex routing, less visible, not cacheable

## Caching Strategy

```kotlin
@ApplicationScoped
class ResponseCache(
    private val redisClient: RedisClient
) {
    
    fun cache(key: String, response: Response, ttlSeconds: Int) {
        redisClient.setex(key, ttlSeconds, response.toJson())
    }
    
    fun get(key: String): Response? {
        return redisClient.get(key)?.let { Response.fromJson(it) }
    }
}

// Usage
@GET
@Path("/api/v1/products/{id}")
fun getProduct(@PathParam("id") id: String): Response {
    val cacheKey = "product:${id}"
    
    return responseCache.get(cacheKey)
        ?: fetchFromBackend(id).also { 
            responseCache.cache(cacheKey, it, ttlSeconds = 300)
        }
}
```

## Security Considerations

1. **HTTPS Only**: Enforce TLS at load balancer
2. **CORS**: Configured per environment
3. **CSRF Protection**: For stateful endpoints
4. **Input Validation**: Basic validation at gateway (detailed in services)
5. **Rate Limiting**: Prevent abuse
6. **DDoS Protection**: Load balancer + CloudFlare/similar

## Monitoring & Metrics

```kotlin
// Metrics to track
- gateway_requests_total{method, path, status, tenant}
- gateway_request_duration_seconds{method, path}
- gateway_rate_limit_exceeded_total{tenant, endpoint}
- gateway_backend_errors_total{service, status}
- gateway_active_connections
```

## Alternatives Considered

### 1. No Gateway (Direct Service Access)
**Rejected**: Security nightmare, no rate limiting, clients must know all service URLs.

### 2. Kong API Gateway
**Rejected**: Another tech stack to learn, less control, not cloud-native for our stack.

### 3. NGINX as Gateway
**Rejected**: Configuration in Lua/config files vs. type-safe Kotlin, limited business logic.

### 4. Spring Cloud Gateway
**Rejected**: Heavier than Quarkus, slower startup, more resource intensive.

### 5. Service Mesh (Istio/Linkerd)
**Deferred**: Adds significant complexity. Will reconsider in Phase 7 if needed for advanced traffic management.

## Related ADRs

- ADR-001: Modular CQRS Implementation
- ADR-003: Event-Driven Integration Between Contexts
- ADR-005: Multi-Tenancy Data Isolation (to be written)
- ADR-007: Authentication and Authorization Strategy (to be written)

## Testing Strategy

- **Unit Tests**: Route matching, rate limiting logic
- **Integration Tests**: End-to-end request flow with mock backends
- **Load Tests**: Identify bottlenecks and capacity limits
- **Security Tests**: Penetration testing, auth bypass attempts

## Performance Targets

- **Latency**: <10ms overhead added by gateway (P95)
- **Throughput**: 10,000 requests/second per gateway instance
- **Availability**: 99.95% uptime
- **Scale**: Horizontally to 10+ instances behind load balancer

## Review Date

- **After Phase 3**: Validate gateway performance under load
- **After Phase 5**: Review if BFF patterns needed
- **Quarterly**: Assess if service mesh needed
