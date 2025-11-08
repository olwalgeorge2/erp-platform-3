# Tenancy & Identity Context

## 1. Purpose
1.1 Provide multi-tenancy, authentication, and authorization services that underpin the platform.
1.2 Manage tenants, users, roles, policies, and delegated administration workflows.
1.3 Deliver identity federation, SSO integrations, and token services for internal and external clients.

## 2. Module Overview
2.1 `identity-application/` - Authentication flows, authorization checks, and identity APIs.
2.2 `identity-domain/` - Tenant, user, role, and policy aggregates with domain services.
2.3 `identity-infrastructure/` - Identity provider connectors, persistence adapters, and cryptographic integrations.

## 3. Integration Highlights
3.1 Issues tokens and tenant context to the API gateway and all bounded contexts.
3.2 Consumes provisioning events from Corporate Services and customer onboarding workflows.
3.3 Emits audit trails and security telemetry to platform observability stacks.

## 4. Authentication Behavior
4.1 Anti-enumeration: authentication failures (unknown user or bad credentials) return a generic `401 AUTHENTICATION_FAILED` without disclosing user existence.
4.2 Timing guard: a minimum response budget is applied to normalize successful vs failed attempts and reduce timing side-channels.
4.3 Lockout & throttling: successive failures increment counters for lockout/throttle policies (see application configuration for thresholds).
4.4 Multi-tenant context: tenant is resolved from the request context (gateway header), and auth decisions are scoped per-tenant.

## 4. Reference
4.1 `docs/ARCHITECTURE.md` (Tenancy & Identity) details the security model and integration points.
4.2 Rollout strategy appears in `docs/ROADMAP.md` Phases 2 and 3.
4.3 Security ADRs and policies reside under `docs/adr/`.
