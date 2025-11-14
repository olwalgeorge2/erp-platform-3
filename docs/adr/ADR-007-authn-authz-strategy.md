# ADR-007: Authentication & Authorization Strategy

Status: Draft  
Date: 2025-11-08  
Context: Tenancy-Identity, API Gateway, Platform-Shared

## Decision

Adopt a layered AuthN/AuthZ strategy:

- Authentication at API Gateway (JWT validation), service trusts claims via headers.
- Authorization enforced inside the bounded context using RBAC (roles) and fine-grained permissions (resource:action[:scope]).
- Tenant scoping enforced per request; SYSTEM_ADMIN role bypasses tenant scoping for operational tasks.
- Anti-enumeration and constant-time guards for login and registration flows.

## Rationale

- Keeps identity services focused on domain rules; gateway centralizes protocol concerns (tokens, rate limits).
- Consistent RBAC model across services via shared semantics; no cross-context coupling.
- Mitigates user enumeration and timing side channels.

## Details

### Gateway Responsibilities
- Validate JWT and set trusted headers for downstream (e.g., `X-User-Id`, `X-User-Roles`, `X-User-Permissions`, `X-Tenant-ID`).
- Enforce rate-limits on `/api/v1/identity/auth/*` and registration endpoints.
- Propagate correlation ID and sanitize gateway-level errors.

### Service Responsibilities (Tenancy-Identity)
- Parse trusted headers into a request principal (RequestPrincipalContext).
- Check tenant access: principal tenant must match path tenant unless role SYSTEM_ADMIN.
- RBAC checks:
  - Roles: SYSTEM_ADMIN, TENANT_ADMIN (baseline)
  - Permissions: `resource:action[:scope]` (e.g., `roles:manage`, `roles:read`)
- Login anti-enumeration: unknown user returns `AUTHENTICATION_FAILED` (401) with timing guard (~100ms minimum).

### Error Handling
- Map domain errors to HTTP status via ResultMapper.
- Sanitize all errors via platform-level ErrorSanitizer; attach `X-Error-ID`.
- Validation errors should also flow through the sanitizer (Phase 2 cleanup).

## Consequences

Positive:
- Cohesive domain logic; consistent enforcement of security concerns.
- Easier gateway policy evolution without changing services.

Risks / Follow-ups:
- All services must standardize on trusted header names and formats.
- Registration anti-enumeration to be completed (success message without leakage).
- Eliminate direct ErrorResponse usages in resources (Phase 2).

## References
- ADR-004: API Gateway Pattern
- ADR-005: Multi-Tenancy Data Isolation Strategy
- ADR-006: Platform-Shared Governance Rules
