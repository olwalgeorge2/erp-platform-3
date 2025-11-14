# Code Review: Phase 2.1 - Bean Validation & Observability (Batch 2)

**Roadmap Reference:** Phase 2 - Cross-Cutting Services → Task 3.1 (tenancy-identity implementation)  
**Review Date:** November 6, 2025  
**Part of:** Batch 1 Refinements (Infrastructure Layer)  
**Priority:** High  
**Focus:** Bean Validation, structured logging, metrics, correlation IDs

---

## Bean Validation Setup

### 1. Add Dependencies

**File:** `identity-application/build.gradle.kts`

```kotlin
dependencies {
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
}
```

### 2. Annotate Commands

**File:** `CreateUserCommand.kt`

```kotlin
package com.erp.identity.application.port.input.command

import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
import jakarta.validation.constraints.*

data class CreateUserCommand(
    @field:NotNull(message = "Tenant ID is required")
    val tenantId: TenantId,
    
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "Username can only contain letters, numbers, underscore, and hyphen"
    )
    val username: String,
    
    @field:Email(message = "Invalid email format")
    @field:NotBlank(message = "Email is required")
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    val password: String,
    
    @field:NotBlank(message = "Full name is required")
    @field:Size(min = 2, max = 200, message = "Full name must be 2-200 characters")
    val fullName: String,
    
    val roleIds: Set<RoleId> = emptySet(),
    val metadata: Map<String, String> = emptyMap(),
)
```

### 3. Add Validation to Service

**File:** `IdentityCommandService.kt`

```kotlin
package com.erp.identity.infrastructure.service

import com.erp.identity.application.port.input.command.CreateUserCommand
import com.erp.identity.application.service.command.UserCommandHandler
import com.erp.identity.domain.model.identity.User
import com.erp.shared.types.results.Result
import io.micrometer.core.annotation.Counted
import io.micrometer.core.annotation.Timed
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import org.slf4j.MDC
import java.util.UUID

@ApplicationScoped
class IdentityCommandService @Inject constructor(
    private val userCommandHandler: UserCommandHandler,
) {
    @Counted(
        value = "identity.user.creation.attempts",
        description = "Total user creation attempts"
    )
    @Timed(
        value = "identity.user.creation.duration",
        description = "User creation duration",
        percentiles = [0.5, 0.95, 0.99]
    )
    @Transactional
    fun createUser(@Valid command: CreateUserCommand): Result<User> {
        val traceId = MDC.get("traceId") ?: UUID.randomUUID().toString()
        
        Log.infof(
            "[%s] Creating user: tenant=%s, username=%s, email=%s",
            traceId, command.tenantId, command.username, command.email
        )
        
        val startTime = System.currentTimeMillis()
        val result = userCommandHandler.createUser(command)
        val duration = System.currentTimeMillis() - startTime
        
        return result.also { r ->
            when (r) {
                is Result.Success -> {
                    Log.infof(
                        "[%s] User created successfully: id=%s, status=%s, duration=%dms",
                        traceId, r.value.id, r.value.status, duration
                    )
                }
                is Result.Failure -> {
                    Log.warnf(
                        "[%s] User creation failed: code=%s, message=%s, duration=%dms",
                        traceId, r.code, r.message, duration
                    )
                }
            }
        }
    }
}
```

---

## Logging Configuration

**File:** `application.properties` (or `application.yml`)

```properties
# Logging
quarkus.log.level=INFO
quarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%t] [%X{traceId}] [%X{tenantId}] %c{2} - %s%e%n
quarkus.log.console.json=false

# Structured JSON logging (optional, for production)
quarkus.log.console.json=true
quarkus.log.console.json.pretty-print=false

# Log correlation
quarkus.log.console.include-location=true

# Metrics
quarkus.micrometer.enabled=true
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/metrics
```

---

## MDC (Correlation ID) Setup

**File:** `RequestLoggingFilter.kt`

```kotlin
package com.erp.identity.infrastructure.web

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import org.slf4j.MDC
import java.util.UUID

@Provider
class RequestLoggingFilter : ContainerRequestFilter, ContainerResponseFilter {
    
    override fun filter(requestContext: ContainerRequestContext) {
        val traceId = requestContext.getHeaderString("X-Trace-ID") 
            ?: UUID.randomUUID().toString()
        
        val tenantId = requestContext.getHeaderString("X-Tenant-ID")
        
        MDC.put("traceId", traceId)
        if (tenantId != null) {
            MDC.put("tenantId", tenantId)
        }
        
        requestContext.setProperty("startTime", System.currentTimeMillis())
    }
    
    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext
    ) {
        val startTime = requestContext.getProperty("startTime") as? Long
        val duration = startTime?.let { System.currentTimeMillis() - it }
        
        responseContext.headers.add("X-Trace-ID", MDC.get("traceId"))
        
        if (duration != null) {
            Log.infof(
                "Request completed: method=%s, path=%s, status=%d, duration=%dms",
                requestContext.method,
                requestContext.uriInfo.path,
                responseContext.status,
                duration
            )
        }
        
        MDC.clear()
    }
}
```

---

## Example Log Output

```
2025-11-06 14:32:15,123 INFO  [http-thread] [f47ac10b-58cc] [a1b2c3d4-e5f6] IdentityCommandService - [f47ac10b-58cc] Creating user: tenant=a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d, username=john_doe, email=john@example.com
2025-11-06 14:32:15,456 INFO  [http-thread] [f47ac10b-58cc] [a1b2c3d4-e5f6] IdentityCommandService - [f47ac10b-58cc] User created successfully: id=9f8e7d6c-5b4a-3c2d-1e0f-9a8b7c6d5e4f, status=PENDING, duration=333ms
```

---

## Metrics Endpoints

After implementation, check:

```bash
# Prometheus metrics
curl http://localhost:8080/metrics

# Should include:
# identity_user_creation_attempts_total{...}
# identity_user_creation_duration_seconds{quantile="0.5",...}
# identity_user_creation_duration_seconds{quantile="0.95",...}
# identity_user_creation_duration_seconds{quantile="0.99",...}
```

---

## Testing

```bash
# Test validation errors
curl -X POST http://localhost:8080/api/v1/identity/users \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "invalid-uuid",
    "username": "ab",
    "email": "not-an-email",
    "password": "short",
    "fullName": "A"
  }'

# Expected: 400 Bad Request with validation errors

# Check logs for trace ID
tail -f logs/application.log | grep "Creating user"

# Check metrics
curl http://localhost:8080/metrics | grep identity_user
```
