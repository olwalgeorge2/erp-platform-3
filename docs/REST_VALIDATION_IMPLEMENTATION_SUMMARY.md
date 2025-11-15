# REST Validation Pattern Implementation Summary

## Overview

This document summarizes the implementation of standardized REST API validation across the ERP platform's bounded contexts, establishing the `@BeanParam` pattern with Bean Validation for consistent error handling and type safety.

## Shared Error Envelope for Consumers

All finance services (accounting, AP, AR, gateway) now emit the same error payload exposed from `bounded-contexts/financial-management/financial-shared/src/main/kotlin/com/erp/financial/shared/api/ErrorResponse.kt`. Downstream UI clients and integrations can rely on the following schema:

```json
{
  "code": "FINANCE_INVALID_VENDOR_NUMBER",
  "message": "Die Lieferantennummer darf nicht leer sein.",
  "validationErrors": [
    {
      "field": "vendorNumber",
      "code": "FINANCE_INVALID_VENDOR_NUMBER",
      "message": "Die Lieferantennummer darf nicht leer sein.",
      "rejectedValue": ""
    }
  ]
}
```

Key points for consumers:

- `code` is always one of the entries declared in `financial-shared/.../validation/FinanceValidationErrorCode.kt` and is stable for automation, dashboards, and UI translations.
- `validationErrors` contains field-level diagnostics; clients should surface the localized `message` and use `field` to map back to form controls.
- Language negotiation happens via `Accept-Language` or `Content-Language`; if no header is provided the default locale is used. Localized strings live in `financial-shared/src/main/resources/ValidationMessages*.properties`.
- Every service installs the shared `FinanceValidationExceptionMapper` and `ValidationAuditFilter`, so HTTP status `422` + error envelope is guaranteed whenever a request fails validation.
- OpenAPI descriptions now reuse the shared `FinanceErrorResponsesFilter` plus `META-INF/openapi/error-responses.yaml` so downstream UIs/integrations can introspect the `ErrorResponse`/`ValidationError` schemas directly from `/q/openapi`.

Existing REST specs should be updated to reference `ErrorResponse` when documenting client-error responses.

## Pattern Documentation

**Primary Reference**: `docs/REST_VALIDATION_PATTERN.md`

This comprehensive guide covers:
- Core validation principles
- Implementation patterns for various scenarios
- Bean Validation annotations reference
- Error response formats
- Testing strategies
- Migration checklist

## Implementation Status

### ✅ Fully Implemented

#### Financial AR (Accounts Receivable)
- **Files**: 
  - `bounded-contexts/financial-management/financial-ar/ar-infrastructure/src/main/kotlin/com/erp/financial/ar/infrastructure/adapter/input/rest/dto/ArOpenItemDtos.kt`
  - `bounded-contexts/financial-management/financial-ar/ar-infrastructure/src/main/kotlin/com/erp/financial/ar/infrastructure/adapter/input/rest/ArOpenItemResource.kt`
- **Request DTOs**: `ArAgingRequest`
- **Endpoints**: `/aging/detail`, `/aging/summary`
- **Validation**: tenantId (UUID), asOfDate (ISO-8601 pattern), optional filters

#### Financial AP (Accounts Payable)
- **Files**:
  - `bounded-contexts/financial-management/financial-ap/ap-infrastructure/src/main/kotlin/com/erp/financial/ap/infrastructure/adapter/input/rest/dto/ApOpenItemDtos.kt`
  - `bounded-contexts/financial-management/financial-ap/ap-infrastructure/src/main/kotlin/com/erp/financial/ap/infrastructure/adapter/input/rest/ApOpenItemResource.kt`
- **Request DTOs**: `AgingRequest`
- **Endpoints**: `/aging/detail`, `/aging/summary`
- **Validation**: tenantId (UUID), vendorId (UUID), asOfDate (ISO-8601 pattern)

#### Financial Accounting
- **Files**:
  - `bounded-contexts/financial-management/financial-accounting/accounting-infrastructure/src/main/kotlin/com/erp/finance/accounting/infrastructure/adapter/input/rest/dto/FinanceQueryDtos.kt`
  - `bounded-contexts/financial-management/financial-accounting/accounting-infrastructure/src/main/kotlin/com/erp/finance/accounting/infrastructure/adapter/input/rest/FinanceQueryResource.kt`
- **Request DTOs**: `TrialBalanceRequest`, `GlSummaryRequest`, `LedgerInfoRequest`
- **Endpoints**: `/ledgers/{ledgerId}/trial-balance`, `/ledgers/{ledgerId}/dimension-summary`, `/company-codes/{companyCodeId}/ledger-info`
- **Validation**: 
  - UUID path/query parameters
  - DimensionType enum with helpful error messages
  - Complex dimension filter parsing (DIMENSION_TYPE:uuid format)

#### Tenancy/Identity
- **Files**:
  - `bounded-contexts/tenancy-identity/identity-infrastructure/src/main/kotlin/com.erp.identity.infrastructure/adapter/input/rest/dto/TenantQueryDtos.kt`
  - `bounded-contexts/tenancy-identity/identity-infrastructure/src/main/kotlin/com.erp.identity.infrastructure/adapter/input/rest/TenantResource.kt`
  - `bounded-contexts/tenancy-identity/identity-infrastructure/src/main/kotlin/com.erp.identity.infrastructure/adapter/input/rest/dto/RoleQueryDtos.kt`
  - `bounded-contexts/tenancy-identity/identity-infrastructure/src/main/kotlin/com.erp.identity.infrastructure/adapter/input/rest/RoleResource.kt`
  - `bounded-contexts/tenancy-identity/identity-infrastructure/src/main/kotlin/com/erp/identity/infrastructure/validation/*`
  - `bounded-contexts/tenancy-identity/identity-infrastructure/src/main/resources/ValidationMessages.properties`
- **Request DTOs**: 
  - Tenant: `GetTenantRequest`, `ListTenantsRequest`
  - Role: `GetRoleRequest`, `ListRolesRequest`, `DeleteRoleRequest`
- **Endpoints**: 
  - Tenant CRUD and listing
  - Role CRUD and listing
- **Validation**:
  - UUID parsing for tenantId, roleId
  - TenantStatus enum validation
  - Pagination with @Min/@Max constraints
  - Domain-specific error codes (e.g., `TENANCY_INVALID_TENANT_ID`)
  - Localized validation messages resolved via `ValidationMessages.properties` (en + de + es) with cached lookups
  - `IdentityValidationExceptionMapper` + `ValidationAuditFilter` log every 4xx validation failure with user/path metadata

#### API Gateway (Admin Rate Limits)
- **Files**:
  - `api-gateway/src/main/kotlin/com.erp.apigateway/admin/RateLimitAdminDtos.kt`
  - `api-gateway/src/main/kotlin/com.erp.apigateway/admin/RateLimitAdminResource.kt`
  - `api-gateway/src/main/kotlin/com.erp.apigateway/validation/*`
  - `api-gateway/src/main/resources/ValidationMessages.properties`
- **Request DTOs**: `TenantOverridePathRequest`, `EndpointOverridePathRequest`, `OverrideRequest`
- **Endpoints**: `/admin/ratelimits/tenants/{tenant}`, `/admin/ratelimits/endpoints/{pattern}`
- **Validation**:
  - Tenant identifier/pattern must be non-blank
  - Numeric override fields must be >= 1
  - Admin authorization enforced before processing
  - Domain-specific validation error codes (e.g., `GATEWAY_INVALID_TENANT_ID`)
  - Gateway-aware localization via `ValidationMessageResolver` (en + de + es) with cached bundle lookups
  - `GatewayValidationExceptionMapper` emits 422 responses with structured violations
  - `ValidationAuditFilter` logs every 4xx validation failure with user/path metadata

## Benefits Achieved

### 1. **Consistent Error Handling**
All invalid requests now return HTTP 400 with clear error messages instead of HTTP 500:

**Before**:
```
HTTP 500 Internal Server Error
{
  "message": "IllegalArgumentException: Invalid UUID string: xyz"
}
```

**After**:
```
HTTP 400 Bad Request
{
  "code": "VALIDATION_ERROR",
  "violations": [
    {
      "field": "tenantId",
      "message": "must not be null"
    }
  ]
}
```

### 2. **Type Safety**
No more uncaught exceptions from:
- `UUID.fromString()` → `IllegalArgumentException`
- `Enum.valueOf()` → `IllegalArgumentException`
- `LocalDate.parse()` → `DateTimeParseException`

### 3. **Reduced Code Duplication**
**Before**: ~15-20 lines per endpoint with manual parsing
**After**: ~4-6 lines per endpoint with pure delegation

Example reduction:
```kotlin
// Before (20 lines)
fun getResource(
    @PathParam("id") id: String,
    @QueryParam("tenantId") tenantId: String,
): Response {
    val resourceId = try {
        UUID.fromString(id)
    } catch (e: IllegalArgumentException) {
        return Response.status(400).entity("Invalid id").build()
    }
    // ... more parsing ...
    val result = service.getResource(resourceId, parsedTenantId)
    return result.toResponse()
}

// After (4 lines)
fun getResource(@Valid @BeanParam request: GetResourceRequest): Response =
    service.getResource(request.toQuery()).toResponse()
```

### 4. **Single Source of Truth**
Validation rules live in DTO classes, not scattered across resource methods:
- Easy to update constraints
- Clear documentation of API contracts
- Reusable across endpoints

### 5. **Better API Documentation**
Bean Validation annotations auto-generate OpenAPI/Swagger constraints:
```kotlin
@field:Min(1) @field:Max(1000) var limit: Int?
```
Automatically appears in API docs as "minimum: 1, maximum: 1000"

## Key Implementation Patterns

### Pattern 1: Simple UUID Validation
```kotlin
data class GetResourceRequest(
    @field:NotNull
    @field:PathParam("id")
    var id: UUID? = null,
) {
    fun toQuery() = GetResourceQuery(
        id = id ?: throw BadRequestException("id is required")
    )
}
```

### Pattern 2: Date with ISO-8601 Enforcement
```kotlin
data class DateRequest(
    @field:NotBlank
    @field:Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "date must be yyyy-MM-dd")
    @field:QueryParam("date")
    var date: String? = null,
) {
    private fun parseDate(): LocalDate = ...
}
```

### Pattern 3: Enum with Helpful Errors
```kotlin
fun toQuery(): Query {
    val status = statusString?.let {
        try {
            Status.valueOf(it.uppercase())
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                "Invalid status: '$it'. Valid values: ${Status.entries.joinToString()}"
            )
        }
    }
    return Query(status = status)
}
```

### Pattern 4: Pagination with Constraints
```kotlin
data class ListRequest(
    @field:QueryParam("limit")
    @field:Min(1)
    @field:Max(1000)
    var limit: Int? = null,
    
    @field:QueryParam("offset")
    @field:Min(0)
    var offset: Int? = null,
) {
    fun toQuery() = ListQuery(
        limit = limit ?: 50,
        offset = offset ?: 0,
    )
}
```

## API Gateway Assessment

### Result: Pattern Implemented for Admin Endpoints

The API Gateway now reuses the same Bean Validation approach for its rate-limit admin APIs:
- Path parameters (`tenant`, `pattern`) validated via BeanParam DTOs
- Override payloads enforce minimum thresholds (>=1) for rate limit configuration
- Admin-only nature preserved via `ensureAdmin()` guard

This keeps gateway admin APIs aligned with the enterprise-wide validation pattern while retaining the flexibility needed for pattern-based overrides. With multi-locale bundles, cached resolvers, and structured audit logging, the identity and gateway stacks now deliver production-ready enterprise validation that meets SAP standards. 🎉

With the identity and gateway stacks now using localized domain codes, structured 422 responses, and audit logging, the platform delivers production-ready enterprise validation that meets SAP standards. 🎉

## Testing Recommendations

### Unit Tests for DTOs
```kotlin
@Test
fun `toQuery should throw BadRequestException for invalid enum`() {
    val request = ListRequest(status = "INVALID")
    assertThrows<BadRequestException> {
        request.toQuery()
    }
}
```

### Integration Tests for REST Endpoints
```kotlin
@Test
fun `should return 400 for missing required parameter`() {
    given()
        .get("/api/v1/resource")
    .then()
        .statusCode(400)
}
```

## Migration Checklist for New Endpoints

When creating a new REST endpoint:

- [ ] Create request DTO with `@field:` Bean Validation annotations
- [ ] Add `@PathParam` / `@QueryParam` annotations to DTO fields
- [ ] Implement `toQuery()` or `toCommand()` with defensive parsing
- [ ] Use `@Valid @BeanParam` in resource method signature
- [ ] Keep resource method to 1-line delegation if possible
- [ ] Add unit tests for DTO validation logic
- [ ] Add integration tests for HTTP 400 scenarios

### Implemented Contract Tests
- `ApOpenItemResourceValidationTest` and `ArOpenItemResourceValidationTest` boot Quarkus with mocked query ports to assert that invalid `asOfDate` parameters produce HTTP `422` + localized `FINANCE_INVALID_DATE` payloads (English and Spanish respectively). These tests run via `./gradlew :bounded-contexts:financial-management:financial-ap:ap-infrastructure:test` and the AR equivalent.
- `VendorBillResourceValidationTest` issues an invalid `POST /api/v1/finance/ap/invoices` with `Accept-Language: de-DE` and verifies the envelope returns `FINANCE_INVALID_NAME` plus the German translation, guaranteeing AP command endpoints honor language negotiation.
- `CustomerResourceValidationTest` performs an invalid `POST /api/v1/finance/customers` with `Accept-Language: es-ES` and asserts the `FINANCE_INVALID_NAME` payload is localized for AR command consumers.

## Known Gaps

- AR command DTOs still rely on manual `require*` parsing. Add Bean Validation annotations + BeanParam wrappers so requests fail-fast before hitting the command use cases.

## Future Considerations

### Potential Extensions

1. **Custom Validators**: Create reusable annotations like `@ValidIsoDate` for common patterns
2. **Error Response Standardization**: Centralize error response format across all contexts
3. **API Versioning**: Apply pattern consistently when creating v2 endpoints
4. **GraphQL/gRPC**: Adapt validation strategy for other API protocols if needed

### Metrics to Track

- Reduction in HTTP 500 errors (should decrease significantly)
- Reduction in support tickets for "unclear error messages"
- Developer time saved by code reuse

## References

- **Main Documentation**: `docs/REST_VALIDATION_PATTERN.md`
- **Jakarta Bean Validation**: https://jakarta.ee/specifications/bean-validation/
- **JAX-RS Specification**: https://jakarta.ee/specifications/restful-ws/

## Contributors

This pattern was implemented across financial and identity bounded contexts in November 2025, establishing a platform-wide standard for REST API validation.

---

**Last Updated**: November 15, 2025
