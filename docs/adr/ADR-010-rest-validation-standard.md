# ADR-010: REST Validation + Domain Error Codes

## Status

Accepted – November 2025

## Context

Each bounded context exposes REST APIs with different validation styles:

- Manual `UUID.fromString` calls in resources leading to `IllegalArgumentException` → HTTP 500
- Mixed error payloads without domain-specific error codes
- No consistent localization or audit logging of validation failures

To reach SAP-grade reliability, identity and API Gateway services now use:

1. **Bean Validation + `@BeanParam` DTOs** that encapsulate parsing and produce commands/queries
2. **Domain validation exceptions** (`IdentityValidationException`, `GatewayValidationException`) with specific error codes
3. **Localized messages** loaded from `ValidationMessages.properties`
4. **Exception mappers + audit filters** that emit structured HTTP 422 responses and log validation events

Finance modules (accounting, AP, AR) still rely on manual parsing. We need a single source of truth describing the REST validation standard so future work can extend it to finance.

## Decision

Adopt the following across all REST APIs:

1. **Validated DTOs**  
   - Every REST endpoint MUST use `@Valid @BeanParam` request objects.  
   - DTOs convert to commands/queries via `toCommand()/toQuery()` and throw domain exceptions on invalid data.

2. **Domain error codes & localization**  
   - Each bounded context defines a `ValidationErrorCode` enum.  
   - DTOs/resources throw `*ValidationException(code, field, rejectedValue, locale)` with message from the local `ValidationMessages.properties`.  
   - Resources derive locale from `HttpHeaders`.

3. **Error envelope**  
   - Exception mappers translate validation exceptions to HTTP 422 with payload:
     ```json
     {
       "code": "TENANCY_INVALID_TENANT_ID",
       "message": "Tenant identifier is invalid.",
       "validationErrors": [
         {"field":"tenantId","code":"TENANCY_INVALID_TENANT_ID","message":"…","rejectedValue":"abc"}
       ]
     }
     ```

4. **Audit logging**  
   - `ValidationAuditFilter` logs every client error (4xx) triggered by validation with user, path, status, and violation codes.

5. **Documentation**  
   - `docs/REST_VALIDATION_PATTERN.md` remains the central guide.  
   - ADR-010 captures the architectural rationale; other ADRs reference it.

## Consequences

### Positive
- Consistent HTTP 400/422 responses across services
- Domain-level error codes enable SAP-style monitoring and automation
- Localization ready via resource bundles
- Audit trail for compliance (SOX, GDPR)
- Reduced boilerplate (resources delegate to validated DTOs)

### Negative / Work Remaining
- Finance modules must be migrated (accounts, AP, AR command/query endpoints)
- Developers must update integration tests to expect HTTP 422 for validation errors

## Follow-Up Tasks

1. Implement validation infra for finance bounded contexts (accounting, AP, AR) mirroring identity/gateway approach.
2. Ensure observability dashboards capture validation error codes across all services.
