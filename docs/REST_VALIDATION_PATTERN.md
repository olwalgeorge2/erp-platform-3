# REST API Validation Pattern

## Overview

This document establishes the standard validation pattern for all REST endpoints across the ERP platform. The pattern ensures consistent error handling, fail-fast validation at the REST boundary, and clean separation between infrastructure and application layers.

## Core Principles

1. **Validate at the boundary** - All validation happens at the REST layer before reaching application/domain logic
2. **Fail fast** - Invalid requests return HTTP 400 with clear error messages immediately
3. **Type safety** - No uncaught parsing exceptions (UUID, enum, date formats)
4. **Single responsibility** - Request DTOs handle validation and conversion to domain queries/commands
5. **Consistency** - Same pattern across all bounded contexts

## The @BeanParam Pattern

### Problem: Manual Validation

**Before** (Anti-pattern):
```kotlin
@GET
@Path("/{id}")
fun getResource(
    @PathParam("id") id: String,
    @QueryParam("tenantId") tenantId: String,
    @QueryParam("status") status: String?,
): Response {
    // Risk: UUID.fromString() throws IllegalArgumentException → 500 error
    val resourceId = UUID.fromString(id)
    val tenant = UUID.fromString(tenantId)
    
    // Risk: valueOf() throws IllegalArgumentException → 500 error
    val statusEnum = status?.let { ResourceStatus.valueOf(it.uppercase()) }
    
    // ... service call
}
```

**Issues:**
- ❌ Uncaught exceptions produce HTTP 500 instead of 400
- ❌ Generic error messages don't help users
- ❌ Parsing logic scattered across resource methods
- ❌ No declarative validation

### Solution: Validated Request DTOs

**After** (Recommended pattern):
```kotlin
// DTO with validation
data class GetResourceRequest(
    @field:NotNull
    @field:PathParam("id")
    var id: UUID? = null,
    
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
    
    @field:QueryParam("status")
    var status: String? = null,
) {
    fun toQuery(): GetResourceQuery {
        val statusEnum = status?.let {
            try {
                ResourceStatus.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                throw BadRequestException(
                    "Invalid status: '$it'. Valid values: ${ResourceStatus.entries.joinToString()}"
                )
            }
        }
        
        return GetResourceQuery(
            id = id ?: throw BadRequestException("id is required"),
            tenantId = tenantId ?: throw BadRequestException("tenantId is required"),
            status = statusEnum,
        )
    }
}

// Resource using validated DTO
@GET
@Path("/{id}")
fun getResource(@Valid @BeanParam request: GetResourceRequest): Response =
    service.getResource(request.toQuery()).toResponse()
```

**Benefits:**
- ✅ Bean Validation catches nulls automatically → HTTP 400
- ✅ Defensive parsing in `toQuery()` provides clear error messages
- ✅ Resource reduced to pure delegation
- ✅ Type-safe conversion to domain types
- ✅ Reusable validation logic

## Implementation Patterns

### Pattern 1: Simple UUID Path/Query Parameters

```kotlin
data class GetItemRequest(
    @field:NotNull
    @field:PathParam("itemId")
    var itemId: UUID? = null,
    
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
) {
    fun toQuery() = GetItemQuery(
        itemId = itemId ?: throw BadRequestException("itemId is required"),
        tenantId = tenantId ?: throw BadRequestException("tenantId is required"),
    )
}
```

### Pattern 2: Date Validation with ISO-8601

```kotlin
data class AgingRequest(
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
    
    @field:NotBlank
    @field:Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "asOfDate must be formatted as yyyy-MM-dd")
    @field:QueryParam("asOfDate")
    var asOfDate: String? = null,
) {
    fun toQuery(): AgingQuery {
        val parsedDate = try {
            LocalDate.parse(asOfDate ?: throw BadRequestException("asOfDate is required"))
        } catch (e: DateTimeParseException) {
            throw BadRequestException("Invalid asOfDate '$asOfDate'. Expected format: yyyy-MM-dd")
        }
        
        return AgingQuery(
            tenantId = tenantId!!,
            asOfDate = parsedDate,
        )
    }
}
```

### Pattern 3: Enum Validation with Helpful Messages

```kotlin
data class ListResourcesRequest(
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
    
    @field:QueryParam("status")
    var status: String? = null,
) {
    fun toQuery(): ListResourcesQuery {
        val statusEnum = status?.let { raw ->
            try {
                ResourceStatus.valueOf(raw.uppercase())
            } catch (e: IllegalArgumentException) {
                throw BadRequestException(
                    "Invalid status: '$raw'. Valid values: ${ResourceStatus.entries.joinToString()}"
                )
            }
        }
        
        return ListResourcesQuery(
            tenantId = tenantId ?: throw BadRequestException("tenantId is required"),
            status = statusEnum,
        )
    }
}
```

### Pattern 4: Complex Parsing with Structured Data

```kotlin
data class TrialBalanceRequest(
    @field:NotNull
    @field:PathParam("ledgerId")
    var ledgerId: UUID? = null,
    
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
    
    @field:QueryParam("dimensionFilter")
    var dimensionFilters: List<String>? = null,
) {
    fun toQuery(): TrialBalanceQuery {
        val filterMap = parseDimensionFilters()
        return TrialBalanceQuery(
            tenantId = tenantId!!,
            ledgerId = ledgerId!!,
            dimensionFilters = filterMap,
        )
    }
    
    private fun parseDimensionFilters(): Map<DimensionType, UUID> {
        if (dimensionFilters.isNullOrEmpty()) return emptyMap()
        
        return dimensionFilters.associate { token ->
            val parts = token.split(":")
            if (parts.size != 2) {
                throw BadRequestException(
                    "dimensionFilter must be formatted as DIMENSION_TYPE:uuid, got: $token"
                )
            }
            
            val type = try {
                DimensionType.valueOf(parts[0].uppercase())
            } catch (e: IllegalArgumentException) {
                throw BadRequestException(
                    "Invalid dimension type: '${parts[0]}'. Valid values: ${DimensionType.entries.joinToString()}"
                )
            }
            
            val value = try {
                UUID.fromString(parts[1])
            } catch (e: IllegalArgumentException) {
                throw BadRequestException("Invalid UUID in dimensionFilter: '${parts[1]}'")
            }
            
            type to value
        }
    }
}
```

### Pattern 5: Pagination Parameters

```kotlin
data class ListRequest(
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
    
    @field:QueryParam("limit")
    @field:Min(1)
    @field:Max(1000)
    var limit: Int? = null,
    
    @field:QueryParam("offset")
    @field:Min(0)
    var offset: Int? = null,
) {
    fun toQuery() = ListQuery(
        tenantId = tenantId ?: throw BadRequestException("tenantId is required"),
        limit = limit ?: 50,
        offset = offset ?: 0,
    )
}
```

## Common Bean Validation Annotations

| Annotation | Use Case | Example |
|------------|----------|---------|
| `@NotNull` | Required UUIDs, objects | `@field:NotNull var tenantId: UUID?` |
| `@NotBlank` | Required strings (no empty/whitespace) | `@field:NotBlank var name: String?` |
| `@NotEmpty` | Required collections/strings (not empty) | `@field:NotEmpty var items: List<String>?` |
| `@Pattern` | String format validation | `@field:Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")` |
| `@Min/@Max` | Numeric range validation | `@field:Min(1) @field:Max(1000) var limit: Int?` |
| `@Size` | Collection/string size | `@field:Size(min = 1, max = 100)` |
| `@Email` | Email format | `@field:Email var email: String?` |
| `@Past/@Future` | Date constraints | `@field:Past var birthDate: LocalDate?` |

## Error Response Format

When validation fails, the JAX-RS/Bean Validation framework automatically returns:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "violations": [
    {
      "field": "tenantId",
      "message": "must not be null"
    },
    {
      "field": "asOfDate",
      "message": "asOfDate must be formatted as yyyy-MM-dd"
    }
  ]
}
```

For custom validation in `toQuery()` methods, use `BadRequestException`:

```kotlin
throw BadRequestException("Invalid status: 'INVALID'. Valid values: ACTIVE, SUSPENDED, TERMINATED")
```

Returns:
```json
{
  "code": "BAD_REQUEST",
  "message": "Invalid status: 'INVALID'. Valid values: ACTIVE, SUSPENDED, TERMINATED"
}
```

## File Organization

### DTO File Naming Convention

- `{Context}QueryDtos.kt` - For query/read operations
- `{Context}CommandDtos.kt` - For command/write operations (if separate from query)

**Examples:**
- `ArOpenItemDtos.kt` - AR aging query DTOs
- `FinanceQueryDtos.kt` - Financial accounting query DTOs
- `TenantQueryDtos.kt` - Tenant query DTOs

### File Structure

```kotlin
package com.erp.{context}.infrastructure.adapter.input.rest.dto

import jakarta.validation.constraints.*
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import java.util.UUID

// Request DTOs with validation
data class SomeRequest(...) { ... }

// Response DTOs (if in same file)
data class SomeResponse(...) { ... }

// Extension functions for conversion
fun DomainResult.toResponse(): SomeResponse = ...
```

## Adoption Across Bounded Contexts

### ✅ Already Implemented

| Bounded Context | Files | Status |
|----------------|-------|--------|
| **Financial AR** | `ArOpenItemDtos.kt`, `ArOpenItemResource.kt` | ✅ Complete |
| **Financial AP** | `ApOpenItemDtos.kt`, `ApOpenItemResource.kt` | ✅ Complete |
| **Financial Accounting** | `FinanceQueryDtos.kt`, `FinanceQueryResource.kt` | ✅ Complete |
| **Tenancy / Identity** | `TenantQueryDtos.kt`, `RoleQueryDtos.kt`, locale-aware validation + mapper/audit filter | ✅ Complete |
| **API Gateway (Admin)** | `RateLimitAdminDtos.kt`, `RateLimitAdminResource.kt`, gateway validation infra | ✅ Complete |

### 🔄 Recommended for Implementation

| Bounded Context | Current State | Recommendation |
|----------------|---------------|----------------|
| **Finance Command/Dimension APIs** | Manual parsing in accounting/AP/AR command endpoints | Implement BeanParam pattern + domain error codes |
| **Other Contexts** | TBD | Assess on case-by-case basis |

## When NOT to Use This Pattern

### Acceptable Exceptions

1. **Admin-only internal APIs** - Gateway admin endpoints with simple string parameters
2. **Health check endpoints** - No validation needed
3. **Static content endpoints** - No parameters
4. **Legacy endpoints** - Document deviation, plan migration

### Alternative: Simple Validation

For very simple endpoints with 1-2 string parameters:

```kotlin
@GET
@Path("/{slug}")
fun getBySlug(@PathParam("slug") @NotBlank slug: String): Response {
    // Direct usage acceptable for admin/internal APIs
    return service.getBySlug(slug).toResponse()
}
```

## Testing Strategy

### Unit Tests for Request DTOs

```kotlin
@Test
fun `toQuery should throw BadRequestException for invalid UUID`() {
    val request = GetResourceRequest(
        id = null,  // Will fail Bean Validation
        tenantId = UUID.randomUUID(),
    )
    
    assertThrows<BadRequestException> {
        request.toQuery()
    }
}

@Test
fun `toQuery should throw BadRequestException for invalid enum`() {
    val request = ListRequest(
        tenantId = UUID.randomUUID(),
        status = "INVALID_STATUS",
    )
    
    val exception = assertThrows<BadRequestException> {
        request.toQuery()
    }
    
    assertTrue(exception.message!!.contains("Valid values:"))
}
```

### Integration Tests for REST Endpoints

```kotlin
@Test
fun `GET endpoint should return 400 for missing required parameter`() {
    given()
        .queryParam("asOfDate", "2025-11-15")
        // Missing tenantId
    .when()
        .get("/api/v1/finance/ar/open-items/aging/detail")
    .then()
        .statusCode(400)
        .body("violations[0].field", equalTo("tenantId"))
}

@Test
fun `GET endpoint should return 400 for invalid UUID format`() {
    given()
        .queryParam("tenantId", "not-a-uuid")
        .queryParam("asOfDate", "2025-11-15")
    .when()
        .get("/api/v1/finance/ar/open-items/aging/detail")
    .then()
        .statusCode(400)
}
```

## Migration Checklist

When converting an existing endpoint:

- [ ] Create request DTO with appropriate `@field:` annotations
- [ ] Add `toQuery()` or `toCommand()` method with defensive parsing
- [ ] Update resource method signature to use `@Valid @BeanParam`
- [ ] Remove manual parsing logic from resource
- [ ] Add/update unit tests for DTO validation
- [ ] Add/update integration tests for HTTP 400 scenarios
- [ ] Update OpenAPI documentation if needed
- [ ] Test with invalid inputs to verify error messages

## Domain-Specific Error Codes, i18n, and Audit Logging

Some bounded contexts require SAP-grade validation metadata (domain error codes, localized text, audit trail). The recommended approach:

1. **Define error codes & exception**  
   - Identity: `bounded-contexts/tenancy-identity/.../validation/ValidationErrorCode.kt`, `IdentityValidationException.kt`  
   - API Gateway: `api-gateway/src/main/kotlin/com.erp.apigateway/validation/GatewayValidationErrorCode.kt`, `GatewayValidationException.kt`

2. **Localized messages**  
   - Store translations in `ValidationMessages.properties` per module (e.g., `bounded-contexts/tenancy-identity/.../resources/ValidationMessages.properties`, `api-gateway/src/main/resources/ValidationMessages.properties`).

3. **DTO/Resource integration**  
   - DTOs call the resolver (e.g., `ValidationMessageResolver`) and throw the domain exception with `Locale` from `HttpHeaders`.

4. **Exception mapper + audit filter**  
   - Map domain exceptions to HTTP 422 with structured validation payloads (`IdentityValidationExceptionMapper`, `GatewayExceptionMapper`).  
   - Log every validation failure via `ValidationAuditFilter`.

Finance modules will follow the same pattern.

## References

- [Jakarta Bean Validation Specification](https://jakarta.ee/specifications/bean-validation/)
- [JAX-RS @BeanParam Documentation](https://jakarta.ee/specifications/restful-ws/)
- Financial AR Implementation: `bounded-contexts/financial-management/financial-ar/ar-infrastructure/src/main/kotlin/com/erp/financial/ar/infrastructure/adapter/input/rest/dto/ArOpenItemDtos.kt`
- Financial AP Implementation: `bounded-contexts/financial-management/financial-ap/ap-infrastructure/src/main/kotlin/com/erp/financial/ap/infrastructure/adapter/input/rest/dto/ApOpenItemDtos.kt`
- Accounting Implementation: `bounded-contexts/financial-management/financial-accounting/accounting-infrastructure/src/main/kotlin/com/erp/finance/accounting/infrastructure/adapter/input/rest/dto/FinanceQueryDtos.kt`
- Architectural Decision Record: `docs/adr/ADR-010-rest-validation-standard.md`

## Summary

This validation pattern provides:
- **Consistency** across all REST APIs
- **Type safety** with no uncaught exceptions
- **Clear error messages** for API consumers
- **Maintainability** through centralized validation logic
- **Clean architecture** by keeping resources thin

All new REST endpoints SHOULD follow this pattern. Existing endpoints SHOULD be migrated during routine maintenance or when bugs related to validation are discovered.
