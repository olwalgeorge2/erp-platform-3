# Error Handling Analysis & Future Policy
**Tenancy-Identity Bounded Context**

---

**Document Information**
- **Author:** Senior Engineering Team
- **Date:** November 7, 2025
- **Version:** 1.0
- **Status:** Policy Recommendation
- **Scope:** Tenancy-Identity Bounded Context → Platform-Wide

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current State Analysis](#2-current-state-analysis)
3. [Security Analysis](#3-security-analysis)
4. [User Experience Analysis](#4-user-experience-analysis)
5. [World-Class Best Practices](#5-world-class-best-practices)
6. [Future Error Handling Policy](#6-future-error-handling-policy)
7. [Implementation Roadmap](#7-implementation-roadmap)
8. [Code Examples & Patterns](#8-code-examples--patterns)
9. [Monitoring & Alerting](#9-monitoring--alerting)
10. [Appendices](#10-appendices)

---

## 1. Executive Summary

### 1.1 Purpose

This document analyzes the current error handling implementation in the tenancy-identity bounded context and proposes a comprehensive, world-class error handling policy focused on:

- **Security:** Preventing information leakage, protecting against enumeration attacks
- **User Experience:** Clear, actionable error messages without technical details
- **Observability:** Rich internal logging for debugging and monitoring
- **Resilience:** Graceful degradation and recovery from failures

### 1.2 Key Findings

**Strengths ✅**
- Excellent Result monad pattern implementation
- Type-safe error handling (no exceptions in business logic)
- Structured error codes (machine-readable)
- Multi-layer validation (domain, application, API)
- Comprehensive logging with trace IDs

**Critical Gaps ⚠️**
- **Security:** Detailed error messages expose internal system details
- **Security:** Enumeration attacks possible (username/email existence)
- **UX:** Technical error codes exposed to end users
- **Observability:** No error classification or severity levels
- **Resilience:** No retry policies or circuit breakers

### 1.3 Recommendation Priority

| Priority | Area | Impact | Effort |
|----------|------|--------|--------|
| **P0** | Sanitize error messages for production | HIGH | MEDIUM |
| **P0** | Implement rate limiting on auth endpoints | HIGH | LOW |
| **P0** | Generic responses for enumeration-sensitive ops | HIGH | LOW |
| **P1** | Error classification system (severity levels) | MEDIUM | MEDIUM |
| **P1** | Centralized error response factory | MEDIUM | LOW |
| **P2** | Retry policies for transient failures | MEDIUM | HIGH |
| **P2** | Circuit breakers for external dependencies | MEDIUM | HIGH |

---

## 2. Current State Analysis

### 2.1 Error Handling Architecture

**Current Implementation:**

```
┌─────────────────────────────────────────────────────────────┐
│                     REST Resource Layer                      │
│  • ResultMapper.kt (HTTP status mapping)                     │
│  • ErrorResponse, ValidationErrorResponse DTOs               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                  Application Service Layer                   │
│  • IdentityCommandService (logging, metrics)                 │
│  • Command handlers (business logic)                         │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                       Domain Layer                           │
│  • Result<T> (Success/Failure monad)                         │
│  • DomainError (code, message, details, cause)               │
│  • ValidationError (field-level errors)                      │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Error Types & Patterns

#### 2.2.1 Domain Errors

**Location:** `platform-shared/common-types/src/main/kotlin/com.erp.shared.types/results/DomainError.kt`

```kotlin
data class DomainError(
    val code: String,              // Machine-readable error code
    val message: String,           // Human-readable message
    val details: Map<String, String> = emptyMap(),  // Context
    val cause: Throwable? = null,  // Root cause
)
```

**Usage Pattern:**
```kotlin
Result.failure(
    code = "USER_NOT_FOUND",
    message = "User not found",
    details = mapOf("userId" to userId.toString())
)
```

**Strengths:**
- ✅ Type-safe, no runtime exceptions
- ✅ Consistent structure across codebase
- ✅ Rich context via details map
- ✅ Chain-able with flatMap/map

**Weaknesses:**
- ⚠️ Message and details exposed to API clients
- ⚠️ No severity classification
- ⚠️ No distinction between internal/external errors

#### 2.2.2 Validation Errors

**Location:** `platform-shared/common-types/src/main/kotlin/com.erp.shared.types/results/ValidationError.kt`

```kotlin
data class ValidationError(
    val field: String,             // Field name (e.g., "password")
    val code: String,              // Error code (e.g., "TOO_SHORT")
    val message: String,           // Human message
    val rejectedValue: String? = null  // Optional rejected value
)
```

**Usage Example (Password Policy):**
```kotlin
ValidationError(
    field = "password",
    code = "TOO_SHORT",
    message = "Password must be at least 12 characters long",
    rejectedValue = password.length.toString()
)
```

**Strengths:**
- ✅ Field-level granularity
- ✅ Clear validation feedback
- ✅ Machine-readable codes

**Weaknesses:**
- ⚠️ `rejectedValue` can leak sensitive data
- ⚠️ No support for nested object validation

#### 2.2.3 HTTP Error Mapping

**Location:** `identity-infrastructure/adapter/input/rest/ResultMapper.kt`

```kotlin
private fun mapStatus(
    code: String,
    hasValidationErrors: Boolean,
): StatusType =
    when {
        hasValidationErrors -> UNPROCESSABLE_ENTITY  // 422
        code.equals("TENANT_NOT_FOUND", ignoreCase = true) -> Status.NOT_FOUND
        code.equals("USER_NOT_FOUND", ignoreCase = true) -> Status.NOT_FOUND
        code.equals("USERNAME_IN_USE", ignoreCase = true) -> Status.CONFLICT
        code.equals("EMAIL_IN_USE", ignoreCase = true) -> Status.CONFLICT
        code.equals("INVALID_CREDENTIALS", ignoreCase = true) -> Status.UNAUTHORIZED
        code.equals("ACCOUNT_LOCKED", ignoreCase = true) -> Status.FORBIDDEN
        else -> Status.BAD_REQUEST  // Default fallback
    }
```

**Current Error Response Format:**
```json
{
  "code": "USERNAME_IN_USE",
  "message": "Username already in use",
  "details": {
    "username": "john.doe"
  },
  "validationErrors": []
}
```

**Strengths:**
- ✅ Consistent HTTP status mapping
- ✅ RESTful semantics (404, 409, 422, etc.)
- ✅ Clear error structure

**Weaknesses:**
- ⚠️ Exposes internal error codes directly
- ⚠️ Details map reveals system internals
- ⚠️ No request/correlation ID in response
- ⚠️ No timestamp

### 2.3 Error Code Inventory

**Current Error Codes in Tenancy-Identity:**

| Error Code | HTTP Status | Layer | Security Risk | User-Facing? |
|------------|-------------|-------|---------------|--------------|
| `TENANT_NOT_FOUND` | 404 | Domain | Medium (enumeration) | Yes |
| `USER_NOT_FOUND` | 404 | Domain | Medium (enumeration) | Yes |
| `ROLE_NOT_FOUND` | 404 | Domain | Low | Yes |
| `USERNAME_IN_USE` | 409 | Domain | **HIGH** (enumeration) | Yes |
| `EMAIL_IN_USE` | 409 | Domain | **HIGH** (enumeration) | Yes |
| `TENANT_SLUG_EXISTS` | 409 | Domain | Medium (enumeration) | Yes |
| `ROLE_NAME_EXISTS` | 409 | Domain | Low | Yes |
| `INVALID_CREDENTIALS` | 401 | Domain | **CRITICAL** (timing attack) | Yes |
| `ACCOUNT_LOCKED` | 403 | Domain | Medium (account status leak) | Yes |
| `USER_NOT_ALLOWED` | 403 | Domain | Low | Yes |
| `WEAK_PASSWORD` | 422 | Domain | Low | Yes |
| `PASSWORD_POLICY_VIOLATION` | 422 | Domain | Low | Yes |
| `ROLE_IMMUTABLE` | 400 | Domain | Low | Yes |
| `TENANT_STATE_INVALID` | 400 | Domain | Medium | Yes |
| `INVALID_TENANT_ID` | 400 | API | Low | Yes |

**Risk Analysis:**

🔴 **CRITICAL Issues:**
- `INVALID_CREDENTIALS` enables username enumeration via timing attacks
- Login response timing differs for existing vs non-existing users

🟠 **HIGH Risk:**
- `USERNAME_IN_USE` and `EMAIL_IN_USE` allow account enumeration
- Attackers can build user database without authentication

🟡 **MEDIUM Risk:**
- `TENANT_NOT_FOUND`, `USER_NOT_FOUND` leak existence information
- `ACCOUNT_LOCKED` reveals account security status

### 2.4 Logging Analysis

**Current Logging Pattern:**

```kotlin
Log.infof(
    "[%s] createUser - tenant=%s, username=%s, email=%s",
    traceId,
    command.tenantId,
    command.username,
    command.email,
)
```

**Success Logging:**
```kotlin
Log.infof(
    "[%s] ✓ %s completed in %d ms - %s",
    traceId,
    operation,
    durationMs,
    successContext(value)
)
```

**Failure Logging:**
```kotlin
Log.warnf(
    "[%s] ✗ %s failed in %d ms - error=%s, %s",
    traceId,
    operation,
    durationMs,
    error.code,
    failureContext(error)
)
```

**Strengths:**
- ✅ Trace ID correlation across requests
- ✅ Timing information for performance monitoring
- ✅ Structured logging format
- ✅ Success/failure distinction

**Weaknesses:**
- ⚠️ Logs may contain PII (email, username)
- ⚠️ No log level policy (when to use INFO vs WARN vs ERROR)
- ⚠️ No sensitive data masking
- ⚠️ Stack traces not captured for infrastructure failures

---

## 3. Security Analysis

### 3.1 OWASP Top 10 Alignment

#### A01:2021 – Broken Access Control
**Status:** ⚠️ PARTIAL

**Current Issues:**
- Missing authorization checks on role endpoints (documented in review)
- Error messages don't distinguish between "not found" and "not authorized"

**Recommendation:**
```kotlin
// Instead of: ROLE_NOT_FOUND
// Use: RESOURCE_NOT_FOUND (generic for both authorization and existence)
```

#### A04:2021 – Insecure Design
**Status:** ⚠️ PARTIAL

**Current Issues:**
- Username/email enumeration via `USERNAME_IN_USE`, `EMAIL_IN_USE`
- Timing attacks possible on authentication

**Recommendation:**
- Use constant-time responses for existence checks
- Generic error: "If this email is registered, you'll receive a notification"

#### A05:2021 – Security Misconfiguration
**Status:** ⚠️ NEEDS ATTENTION

**Current Issues:**
- No environment-based error message filtering
- Same detailed errors in dev, staging, and production

**Recommendation:**
```kotlin
// Environment-aware error sanitization
if (isProduction) {
    sanitizeErrorResponse(error)
} else {
    detailedErrorResponse(error)
}
```

#### A09:2021 – Security Logging and Monitoring Failures
**Status:** ✅ GOOD (with improvements needed)

**Strengths:**
- Comprehensive logging with trace IDs
- Structured logging format

**Improvements Needed:**
- Security event classification
- Alerting on suspicious patterns (multiple failed logins)
- PII masking in logs

### 3.2 Enumeration Attack Vectors

#### 3.2.1 Username Enumeration

**Current Vulnerability:**

```http
POST /api/v1/users
{
  "username": "existing.user",
  ...
}

Response: 409 Conflict
{
  "code": "USERNAME_IN_USE",
  "message": "Username already in use",
  "details": {
    "username": "existing.user"
  }
}
```

**Attack Scenario:**
1. Attacker iterates common usernames
2. System confirms which usernames exist
3. Targeted attacks on confirmed accounts

**Secure Alternative:**
```http
Response: 200 OK
{
  "message": "Registration request received. If the email is available, you'll receive a confirmation."
}
```

#### 3.2.2 Email Enumeration

**Current Vulnerability:**

```http
POST /api/v1/users
{
  "email": "victim@company.com",
  ...
}

Response: 409 Conflict
{
  "code": "EMAIL_IN_USE",
  "message": "Email already in use"
}
```

**Impact:**
- Attackers build email database for phishing
- Competitive intelligence (employee enumeration)
- GDPR/privacy violations

**Secure Alternative:**
```http
Response: 202 Accepted
{
  "message": "Registration submitted. Check your email for confirmation."
}

// Backend: Send different emails based on whether account exists
// Existing: "Someone tried to register with your email"
// New: "Welcome! Confirm your account"
```

#### 3.2.3 Tenant/Account Discovery

**Current Vulnerability:**

```http
GET /api/v1/tenants/00000000-0000-0000-0000-000000000000

Response: 404 Not Found
{
  "code": "TENANT_NOT_FOUND",
  "message": "Tenant not found"
}
```

**vs Valid Tenant:**
```http
GET /api/v1/tenants/{valid-uuid}

Response: 403 Forbidden (if not authorized)
or 200 OK (if authorized)
```

**Attack:** Attacker can distinguish between:
- Tenant doesn't exist → 404
- Tenant exists but no access → 403

**Secure Alternative:**
```http
// For both non-existent and unauthorized:
Response: 404 Not Found
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Resource not found"
}
```

### 3.3 Information Leakage Analysis

#### 3.3.1 Technical Details in Errors

**Current Issue:**

```kotlin
// TenantCommandHandler.kt
Result.failure(
    code = "TENANT_STATE_INVALID",
    message = it.message ?: "Invalid tenant state transition",
    details = mapOf("tenantId" to tenantId.toString()),
    cause = it  // Exception details!
)
```

**Risk:** Exception stack traces, database constraint names, internal IDs exposed

**Secure Pattern:**
```kotlin
// Log full details internally
logger.error("Tenant state transition failed", it)

// Return sanitized error to client
Result.failure(
    code = "OPERATION_FAILED",
    message = "Unable to complete operation",
    details = emptyMap()  // No internal details
)
```

#### 3.3.2 Database Constraint Leakage

**Example:**
```
PostgreSQL constraint violation: "uk_tenant_slug"
```

Reveals:
- Database type (PostgreSQL)
- Schema structure (constraint naming)
- Table relationships

**Solution:** Map database errors to generic business errors

### 3.4 Timing Attack Analysis

**Current Authentication Code:**

```kotlin
fun authenticate(user: User, rawPassword: String): AuthenticationResult {
    // Different code paths = different timings
    if (!user.canLogin()) {
        return AuthenticationResult.Failure(...)  // Fast path
    }
    
    if (!credentialVerifier.verify(rawPassword, user.credential)) {
        // Slow path (hashing + comparison)
        val updatedUser = user.recordFailedLogin()
        ...
    }
}
```

**Vulnerability:**
- Non-existent users: Fast rejection
- Existing users: Slow password verification
- Attacker can time responses to enumerate users

**Secure Pattern:**
```kotlin
fun authenticate(username: String, rawPassword: String): AuthenticationResult {
    // Always perform full timing
    val user = userRepository.findByUsername(username)
        ?: createDummyUser()  // Fake user with dummy hash
    
    // Always verify (even for non-existent users)
    val verified = credentialVerifier.verify(rawPassword, user.credential)
    
    // Constant-time response
    return if (verified && user.isReal()) {
        AuthenticationResult.Success(user)
    } else {
        AuthenticationResult.Failure(GENERIC_AUTH_ERROR)
    }
}
```

---

## 4. User Experience Analysis

### 4.1 Current Error Messages (User Perspective)

#### 4.1.1 Good Examples ✅

**Password Validation:**
```json
{
  "code": "WEAK_PASSWORD",
  "message": "Password does not meet requirements",
  "validationErrors": [
    {
      "field": "password",
      "code": "TOO_SHORT",
      "message": "Password must be at least 12 characters long",
      "rejectedValue": "8"
    },
    {
      "field": "password",
      "code": "MISSING_SPECIAL",
      "message": "Password must contain at least one special character",
      "rejectedValue": null
    }
  ]
}
```

**Why It's Good:**
- ✅ Clear, actionable feedback
- ✅ Specific field-level guidance
- ✅ No technical jargon
- ✅ Helps user fix the issue

#### 4.1.2 Poor Examples ⚠️

**Example 1: Technical Error Code**
```json
{
  "code": "TENANT_STATE_INVALID",
  "message": "Invalid tenant state transition",
  "details": {
    "tenantId": "123e4567-e89b-12d3-a456-426614174000"
  }
}
```

**Problems:**
- ❌ "State transition" is technical jargon
- ❌ UUID means nothing to end users
- ❌ No guidance on how to fix
- ❌ No explanation of what happened

**Better Version:**
```json
{
  "message": "This action cannot be performed at this time. Please contact support if you need assistance.",
  "supportReference": "ERR-2025-1107-A4F3"
}
```

**Example 2: Internal System Details**
```json
{
  "code": "ROLE_IMMUTABLE",
  "message": "System roles cannot be modified",
  "details": {
    "roleId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**Problems:**
- ❌ "Immutable" is technical term
- ❌ Role ID is meaningless to users
- ❌ No alternative action suggested

**Better Version:**
```json
{
  "message": "This is a system role and cannot be changed. Create a new custom role instead.",
  "suggestion": "You can create a custom role with similar permissions."
}
```

### 4.2 Error Message Quality Matrix

| Aspect | Current | Target | Gap |
|--------|---------|--------|-----|
| **Clarity** | Technical codes exposed | Plain language | HIGH |
| **Actionability** | Limited guidance | Clear next steps | MEDIUM |
| **Context** | Internal IDs | User-relevant info | HIGH |
| **Tone** | System-centric | User-friendly | MEDIUM |
| **Localization** | English only | Multi-language support | HIGH |
| **Consistency** | Varied formats | Unified structure | LOW |

### 4.3 Error Recovery Guidance

**Current State:** Minimal recovery guidance

**Best Practice Examples:**

#### Authentication Failure
**Current:**
```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "Credentials are invalid"
}
```

**Enhanced:**
```json
{
  "message": "The email or password you entered is incorrect.",
  "suggestions": [
    "Check your email and password for typos",
    "Use 'Forgot Password' if you can't remember your password",
    "Contact support if you continue having trouble"
  ],
  "actions": [
    {
      "label": "Forgot Password?",
      "url": "/auth/forgot-password"
    }
  ]
}
```

#### Account Locked
**Current:**
```json
{
  "code": "ACCOUNT_LOCKED",
  "message": "Account is locked due to repeated failures",
  "details": {
    "failedAttempts": "5"
  }
}
```

**Enhanced:**
```json
{
  "message": "Your account has been temporarily locked for security reasons.",
  "explanation": "We've detected multiple unsuccessful login attempts.",
  "recovery": {
    "automatic": "Your account will be automatically unlocked in 30 minutes.",
    "manual": "Or contact support for immediate assistance."
  },
  "actions": [
    {
      "label": "Contact Support",
      "url": "/support/unlock-account"
    }
  ]
}
```

### 4.4 User-Friendly Error Categories

**Proposed Error Categories for End Users:**

| Category | User Message Template | Internal Mapping |
|----------|----------------------|------------------|
| **Validation** | "Please check: {field} {issue}" | WEAK_PASSWORD, TOO_SHORT, etc. |
| **Not Found** | "We couldn't find that resource" | *_NOT_FOUND errors |
| **Conflict** | "That {resource} is already taken" | *_IN_USE, *_EXISTS errors |
| **Permission** | "You don't have permission for this action" | Authorization failures |
| **Authentication** | "Please check your login credentials" | INVALID_CREDENTIALS |
| **Rate Limit** | "Too many requests. Please try again in {time}" | RATE_LIMIT_EXCEEDED |
| **Server Error** | "Something went wrong. We're working on it." | 5xx errors |

---

## 5. World-Class Best Practices

### 5.1 Industry Standards

#### 5.1.1 OWASP Error Handling Best Practices

**Source:** OWASP Application Security Verification Standard (ASVS) 4.0

**V7.4 Error Handling and Logging**

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| 7.4.1: Generic error messages to users | ⚠️ PARTIAL | Need sanitization layer |
| 7.4.2: Detailed logging internally | ✅ DONE | Comprehensive logging exists |
| 7.4.3: No stack traces in production | ⚠️ MISSING | Need environment check |
| 7.4.4: Error handling in all layers | ✅ DONE | Result monad everywhere |

**Key Principles:**
1. **Separation of Concerns:** Different error details for users vs developers
2. **Fail Securely:** Default to deny/generic error
3. **No Information Leakage:** No technical details, paths, or stack traces
4. **Correlation:** Use unique error IDs for support tracking

#### 5.1.2 NIST Guidelines

**Source:** NIST SP 800-53 Rev. 5 - Security and Privacy Controls

**SI-11: Error Handling**
- Generate error messages that provide necessary information without revealing sensitive data
- Reveal error messages only to authorized personnel
- Implement alternative display mechanisms for security-relevant errors

**AU-3: Content of Audit Records**
- Include error details in audit logs
- Maintain separate user-facing and audit trails
- Protect audit information from unauthorized access

#### 5.1.3 Cloud Provider Patterns

**AWS Best Practices:**
```json
{
  "errorType": "ValidationException",
  "errorMessage": "User-friendly message",
  "requestId": "abc-123-def-456"
}
```

**Google Cloud:**
```json
{
  "error": {
    "code": 400,
    "message": "User-visible error message",
    "status": "INVALID_ARGUMENT"
  }
}
```

**Azure (RFC 7807):**
```json
{
  "type": "https://api.example.com/errors/validation",
  "title": "Your request could not be processed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/users/create",
  "traceId": "00-abc123-def456-00"
}
```

### 5.2 RFC 7807 - Problem Details Standard

```json
{
  "type": "https://api.erp.com/problems/validation-error",
  "title": "Your input contains errors",
  "status": 422,
  "detail": "Password does not meet security requirements",
  "instance": "/api/v1/users",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "invalidParams": [
    {
      "name": "password",
      "reason": "Must be at least 12 characters"
    }
  ]
}
```

### 5.3 Error Classification Systems

#### 5.3.1 Severity Levels

```kotlin
enum class ErrorSeverity {
    DEBUG,      // Development issues
    INFO,       // Expected errors (duplicate username)
    WARNING,    // Recoverable issues
    ERROR,      // Business logic failures
    CRITICAL    // System failures
}
```

**Severity Mapping:**

| Error Type | Severity | Alert? | User Impact |
|------------|----------|--------|-------------|
| Validation errors | INFO | No | Low - user can fix |
| Not found | INFO | No | Low - user input issue |
| Conflict (duplicate) | INFO | No | Low - user can choose different |
| Authentication failure | WARNING | Yes (pattern) | Medium - security event |
| Authorization failure | WARNING | Yes | Medium - potential attack |
| State transition error | ERROR | Yes | Medium - business logic issue |
| Database connection | CRITICAL | Yes (immediate) | High - system down |

#### 5.3.2 Error Categories

```kotlin
enum class ErrorCategory {
    VALIDATION,           // User input issues
    AUTHENTICATION,       // Login/credentials
    AUTHORIZATION,        // Permissions
    RESOURCE_NOT_FOUND,   // Entity doesn't exist
    CONFLICT,             // Resource state conflict
    RATE_LIMIT,           // Too many requests
    BUSINESS_RULE,        // Domain logic violation
    EXTERNAL_SERVICE,     // Third-party API failure
    DATABASE,             // Persistence layer
    SYSTEM                // Infrastructure/unknown
}
```

---

## 6. Future Error Handling Policy

### 6.1 Policy Overview

**Objective:** Establish a comprehensive, secure, and user-friendly error handling framework that:
1. **Protects** sensitive system information from unauthorized disclosure
2. **Guides** users with clear, actionable error messages
3. **Enables** effective debugging and monitoring
4. **Ensures** compliance with security standards (OWASP, NIST)

### 6.2 Core Principles

#### Principle 1: Dual-Layer Error Handling

**Separate Internal and External Error Representations**

```kotlin
// Internal representation (full details)
data class InternalError(
    val code: String,
    val message: String,
    val category: ErrorCategory,
    val severity: ErrorSeverity,
    val details: Map<String, Any>,
    val stackTrace: String?,
    val cause: Throwable?,
    val timestamp: Instant,
    val traceId: String,
    val tenantId: String?,
    val userId: String?
)

// External representation (sanitized)
data class ExternalError(
    val message: String,              // User-friendly message
    val errorId: String,              // Support reference
    val timestamp: Instant,
    val suggestions: List<String>?,   // Recovery guidance
    val actions: List<ErrorAction>?   // Actionable links
)
```

#### Principle 2: Environment-Aware Error Responses

```kotlin
fun toExternalError(
    internal: InternalError,
    environment: Environment
): ExternalError {
    return when (environment) {
        Environment.DEVELOPMENT -> {
            // Detailed errors for debugging
            ExternalError(
                message = "${internal.code}: ${internal.message}",
                errorId = internal.traceId,
                details = internal.details,  // Include details
                stackTrace = internal.stackTrace  // Include stack trace
            )
        }
        Environment.STAGING -> {
            // Moderate details
            ExternalError(
                message = internal.message,
                errorId = internal.traceId,
                details = sanitize(internal.details)  // Partial details
            )
        }
        Environment.PRODUCTION -> {
            // Minimal, user-friendly errors
            ExternalError(
                message = getUserFriendlyMessage(internal.category),
                errorId = internal.traceId,
                suggestions = getRecoveryGuidance(internal.code)
            )
        }
    }
}
```

#### Principle 3: Zero Information Leakage

**Security-Sensitive Operations:**

```kotlin
// DON'T: Reveal existence
if (user == null) {
    return error("USER_NOT_FOUND")
}

// DO: Generic response
fun checkCredentials(email: String, password: String): AuthResult {
    // Always perform full authentication flow
    val user = repository.findByEmail(email) ?: createDummyUser()
    val verified = verifyPassword(password, user.passwordHash)
    
    return if (verified && user.exists) {
        AuthResult.Success(user)
    } else {
        AuthResult.Failure(GENERIC_AUTH_ERROR)  // Same error for all failures
    }
}
```

#### Principle 4: Correlation & Traceability

**Every Error Must Have:**
1. **Unique Error ID** - For support ticket correlation
2. **Trace ID** - For distributed tracing
3. **Timestamp** - For temporal analysis
4. **Context** - Tenant, user, operation

```kotlin
data class ErrorContext(
    val errorId: UUID = UUID.randomUUID(),
    val traceId: String = MDC.get("traceId"),
    val timestamp: Instant = Instant.now(),
    val tenantId: String? = MDC.get("tenantId"),
    val userId: String? = MDC.get("userId"),
    val requestPath: String,
    val httpMethod: String
)
```

### 6.3 Error Message Standards

#### 6.3.1 User-Facing Messages

**Requirements:**
- ✅ Plain language (no technical jargon)
- ✅ Actionable (tell users what to do)
- ✅ Specific (what went wrong)
- ✅ Empathetic (acknowledge frustration)
- ❌ No internal details (IDs, codes, stack traces)
- ❌ No technical implementation details

**Examples:**

| Scenario | ❌ Bad | ✅ Good |
|----------|-------|---------|
| Validation | "WEAK_PASSWORD: min 12 chars" | "Your password needs to be at least 12 characters long" |
| Not Found | "TENANT_NOT_FOUND: {uuid}" | "We couldn't find that organization" |
| Conflict | "USERNAME_IN_USE: john.doe" | "That username is already taken. Please try another" |
| Auth Failure | "INVALID_CREDENTIALS" | "The email or password is incorrect. Please try again" |
| System Error | "NPE at line 42" | "Something went wrong. We're working on it. (Ref: ERR-A4F3)" |

#### 6.3.2 Internal Logging Messages

**Requirements:**
- ✅ Include error code
- ✅ Include full context (IDs, values)
- ✅ Include stack traces for exceptions
- ✅ Include timing information
- ✅ Structured format (JSON)
- ⚠️ Mask sensitive data (passwords, tokens)

**Example:**
```json
{
  "level": "ERROR",
  "timestamp": "2025-11-07T14:23:45.123Z",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "errorId": "ERR-2025-1107-A4F3",
  "errorCode": "USER_CREATION_FAILED",
  "errorCategory": "DATABASE",
  "errorSeverity": "ERROR",
  "message": "Failed to create user due to database constraint violation",
  "context": {
    "tenantId": "123e4567-e89b-12d3-a456-426614174000",
    "username": "john.doe",
    "email": "john@example.com",
    "operation": "createUser",
    "durationMs": 145
  },
  "exception": {
    "type": "PersistenceException",
    "message": "Unique constraint violation: uk_user_email",
    "stackTrace": "..."
  }
}
```

### 6.4 Security Policies

#### Policy 1: Anti-Enumeration

**For operations that can reveal resource existence:**

```kotlin
// Username/Email Registration
fun registerUser(request: RegisterRequest): Response {
    // Check if user exists
    val exists = userRepository.existsByEmail(request.email)
    
    if (exists) {
        // Send "account already exists" email to existing user
        emailService.sendAccountExistsNotification(request.email)
    } else {
        // Create new user and send welcome email
        val user = userService.createUser(request)
        emailService.sendWelcomeEmail(user)
    }
    
    // ALWAYS return same response
    return Response.accepted()
        .entity(mapOf(
            "message" to "Registration received. Check your email for next steps."
        ))
        .build()
}
```

#### Policy 2: Constant-Time Authentication

**Prevent timing attacks:**

```kotlin
suspend fun authenticate(
    email: String,
    password: String
): AuthenticationResult {
    val startTime = System.nanoTime()
    
    // Always fetch or create dummy user
    val user = userRepository.findByEmail(email)
        ?: User.createDummy(email)
    
    // Always verify password (even for non-existent users)
    val verified = passwordHasher.verify(
        password,
        user.passwordHash
    )
    
    // Add minimum delay to prevent timing analysis
    val elapsed = System.nanoTime() - startTime
    val minimumDelayNs = 100_000_000 // 100ms
    if (elapsed < minimumDelayNs) {
        delay((minimumDelayNs - elapsed) / 1_000_000)
    }
    
    return if (verified && user.isReal) {
        AuthenticationResult.Success(user)
    } else {
        // Record failed attempt (if real user)
        if (user.isReal) {
            securityService.recordFailedLogin(user.id)
        }
        AuthenticationResult.Failure(GENERIC_AUTH_ERROR)
    }
}
```

#### Policy 3: Rate Limiting Error Disclosure

**Limit information leakage through rate limiting:**

```kotlin
@RateLimited(
    key = "auth:#{@request.getRemoteAddr()}",
    limit = 5,
    window = Duration.ofMinutes(15)
)
fun authenticate(credentials: Credentials): Response {
    // Authentication logic
}

// When rate limit exceeded
{
  "message": "Too many attempts. Please try again in 15 minutes.",
  "retryAfter": "2025-11-07T14:38:00Z"
}
```

### 6.5 PII Protection in Errors

**Policy:** Never include PII in external error responses or unencrypted logs

**Sensitive Fields:**
- Email addresses
- Phone numbers
- Physical addresses
- Names (in some contexts)
- IP addresses (in some jurisdictions)
- Session tokens, API keys, passwords (ALWAYS)

**Masking Strategy:**
```kotlin
fun maskEmail(email: String): String {
    val parts = email.split("@")
    if (parts.size != 2) return "***@***"
    
    val local = parts[0]
    val domain = parts[1]
    
    val maskedLocal = if (local.length <= 2) {
        "*".repeat(local.length)
    } else {
        local.take(2) + "*".repeat(local.length - 2)
    }
    
    return "$maskedLocal@$domain"  // "jo***@example.com"
}

fun maskSensitiveData(details: Map<String, Any>): Map<String, Any> {
    return details.mapValues { (key, value) ->
        when {
            key.contains("email", ignoreCase = true) -> maskEmail(value.toString())
            key.contains("password", ignoreCase = true) -> "***"
            key.contains("token", ignoreCase = true) -> "***"
            key.contains("ssn", ignoreCase = true) -> "***-**-${value.toString().takeLast(4)}"
            else -> value
        }
    }
}
```

---

## 7. Implementation Roadmap

### 7.1 Phase 1: Critical Security Fixes (Week 1-2)

**Priority: P0 - Must Fix Before Production**

#### Task 1.1: Implement Error Response Sanitizer

**File:** `platform-shared/common-types/src/main/kotlin/com.erp.shared.types/errors/ErrorSanitizer.kt`

```kotlin
package com.erp.shared.types.errors

import com.erp.shared.types.results.DomainError
import com.erp.shared.types.results.ValidationError
import java.time.Instant
import java.util.UUID

object ErrorSanitizer {
    private val SENSITIVE_ERROR_CODES = setOf(
        "USERNAME_IN_USE",
        "EMAIL_IN_USE",
        "TENANT_SLUG_EXISTS",
        "USER_NOT_FOUND",
        "TENANT_NOT_FOUND"
    )
    
    fun sanitize(
        error: DomainError,
        validationErrors: List<ValidationError>,
        environment: Environment
    ): SanitizedError {
        return when (environment) {
            Environment.PRODUCTION -> sanitizeForProduction(error, validationErrors)
            Environment.STAGING -> sanitizeForStaging(error, validationErrors)
            Environment.DEVELOPMENT -> noSanitization(error, validationErrors)
        }
    }
    
    private fun sanitizeForProduction(
        error: DomainError,
        validationErrors: List<ValidationError>
    ): SanitizedError {
        // For enumeration-sensitive errors, use generic messages
        if (error.code in SENSITIVE_ERROR_CODES) {
            return SanitizedError(
                message = getGenericMessage(error.code),
                errorId = UUID.randomUUID().toString(),
                timestamp = Instant.now(),
                validationErrors = emptyList(),  // Don't expose validation details
                suggestions = getRecoveryGuidance(error.code)
            )
        }
        
        // For other errors, use user-friendly message without details
        return SanitizedError(
            message = getUserFriendlyMessage(error.code, error.message),
            errorId = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            validationErrors = validationErrors.map { sanitizeValidationError(it) },
            suggestions = getRecoveryGuidance(error.code)
        )
    }
    
    private fun sanitizeValidationError(error: ValidationError): SanitizedValidationError {
        return SanitizedValidationError(
            field = error.field,
            message = error.message,  // Keep user-friendly message
            code = null  // Don't expose internal codes
        )
    }
    
    private fun getGenericMessage(code: String): String {
        return when (code) {
            "USERNAME_IN_USE", "EMAIL_IN_USE" -> 
                "We couldn't complete your registration. Please try again or contact support."
            "USER_NOT_FOUND", "TENANT_NOT_FOUND" -> 
                "We couldn't find that resource."
            "TENANT_SLUG_EXISTS" -> 
                "That organization name is not available."
            else -> 
                "We couldn't complete your request. Please try again later."
        }
    }
    
    private fun getUserFriendlyMessage(code: String, technicalMessage: String): String {
        return USER_FRIENDLY_MESSAGES[code] ?: "An error occurred. Please try again."
    }
    
    private fun getRecoveryGuidance(code: String): List<String>? {
        return RECOVERY_GUIDANCE[code]
    }
    
    private val USER_FRIENDLY_MESSAGES = mapOf(
        "WEAK_PASSWORD" to "Your password doesn't meet our security requirements.",
        "INVALID_CREDENTIALS" to "The email or password you entered is incorrect.",
        "ACCOUNT_LOCKED" to "Your account has been temporarily locked for security reasons.",
        "ROLE_IMMUTABLE" to "This role cannot be modified because it's a system role.",
        "TENANT_STATE_INVALID" to "This action cannot be performed at this time.",
        "ROLE_NOT_FOUND" to "That role doesn't exist.",
        "ROLE_NAME_EXISTS" to "A role with that name already exists."
    )
    
    private val RECOVERY_GUIDANCE = mapOf(
        "INVALID_CREDENTIALS" to listOf(
            "Double-check your email and password for typos",
            "Use 'Forgot Password' if you can't remember your password",
            "Contact support if you continue having trouble"
        ),
        "ACCOUNT_LOCKED" to listOf(
            "Wait 30 minutes for automatic unlock",
            "Contact support for immediate assistance"
        ),
        "WEAK_PASSWORD" to listOf(
            "Use at least 12 characters",
            "Include uppercase and lowercase letters",
            "Include at least one number",
            "Include at least one special character (!@#$%^&*)"
        )
    )
}

data class SanitizedError(
    val message: String,
    val errorId: String,
    val timestamp: Instant,
    val validationErrors: List<SanitizedValidationError>,
    val suggestions: List<String>? = null,
    val actions: List<ErrorAction>? = null
)

data class SanitizedValidationError(
    val field: String,
    val message: String,
    val code: String? = null
)

data class ErrorAction(
    val label: String,
    val url: String,
    val method: String = "GET"
)

enum class Environment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}
```

**Estimated Effort:** 2 days
**Dependencies:** None
**Testing:** Unit tests for all error code mappings

#### Task 1.2: Update ResultMapper to Use Sanitizer

**File:** `identity-infrastructure/adapter/input/rest/ResultMapper.kt`

```kotlin
package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.shared.types.errors.ErrorSanitizer
import com.erp.shared.types.errors.Environment
import com.erp.shared.types.results.Result
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.config.inject.ConfigProperty

class ResultMapper {
    @ConfigProperty(name = "app.environment", defaultValue = "PRODUCTION")
    lateinit var environment: String
    
    fun <T, R> Result<T>.toResponse(
        successStatus: Response.Status = Response.Status.OK,
        transform: (T) -> R
    ): Response =
        when (this) {
            is Result.Success ->
                Response
                    .status(successStatus)
                    .entity(transform(value))
                    .build()
            is Result.Failure -> {
                val sanitized = ErrorSanitizer.sanitize(
                    error = error,
                    validationErrors = validationErrors,
                    environment = Environment.valueOf(environment)
                )
                
                Response
                    .status(mapStatus(error.code, validationErrors.isNotEmpty()))
                    .entity(sanitized)
                    .build()
            }
        }
}
```

**Estimated Effort:** 1 day
**Dependencies:** Task 1.1
**Testing:** Integration tests with different environments

#### Task 1.3: Implement Constant-Time Authentication

**File:** `identity-domain/services/AuthenticationService.kt`

```kotlin
class AuthenticationService(
    private val credentialVerifier: CredentialVerifier,
    private val passwordPolicy: PasswordPolicy = PasswordPolicy()
) {
    private val MINIMUM_AUTH_TIME_MS = 100L  // Prevent timing attacks
    
    suspend fun authenticate(
        email: String,
        rawPassword: String,
        userProvider: suspend (String) -> User?
    ): AuthenticationResult {
        val startTime = System.currentTimeMillis()
        
        // Always fetch or create dummy user
        val user = userProvider(email) ?: createDummyUser(email)
        
        // Always verify password (even for dummy users)
        val verified = credentialVerifier.verify(rawPassword, user.credential)
        
        // Ensure minimum processing time
        ensureMinimumTime(startTime, MINIMUM_AUTH_TIME_MS)
        
        return if (verified && user.isReal && user.canLogin()) {
            val authenticatedUser = user.recordSuccessfulLogin()
            AuthenticationResult.Success(authenticatedUser)
        } else {
            // Record failed attempt for real users only
            val updatedUser = if (!user.isDummy) {
                user.recordFailedLogin()
            } else {
                user
            }
            
            AuthenticationResult.Failure(
                user = updatedUser,
                reason = GENERIC_AUTH_ERROR
            )
        }
    }
    
    private fun createDummyUser(email: String): User {
        return User(
            id = UserId.generate(),
            tenantId = TenantId.generate(),
            username = "dummy",
            email = email,
            credential = Credential(
                passwordHash = "\$argon2id\$v=19\$m=65536,t=2,p=1\$dummysalt\$dummyhash",
                salt = "dummy",
                algorithm = HashAlgorithm.ARGON2,
                lastChangedAt = Instant.now()
            ),
            status = UserStatus.INACTIVE,
            isDummy = true  // Flag to identify dummy users
        )
    }
    
    private suspend fun ensureMinimumTime(startTime: Long, minimumMs: Long) {
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < minimumMs) {
            delay(minimumMs - elapsed)
        }
    }
    
    companion object {
        private val GENERIC_AUTH_ERROR = DomainError(
            code = "AUTHENTICATION_FAILED",
            message = "Authentication failed"
        )
        
        private val ACCOUNT_STATUS_ERROR = DomainError(
            code = "ACCOUNT_UNAVAILABLE",
            message = "Account unavailable"
        )
    }
}
```

**Estimated Effort:** 3 days
**Dependencies:** None
**Testing:** 
- Unit tests for timing consistency
- Security tests for timing attack prevention

#### Task 1.4: Implement Anti-Enumeration for Registration

**File:** `identity-infrastructure/adapter/input/rest/UserResource.kt`

```kotlin
@Path("/api/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class UserResource(
    private val commandService: IdentityCommandService,
    private val emailService: EmailService
) {
    @POST
    @Transactional
    suspend fun registerUser(request: RegisterUserRequest): Response {
        val existingUser = commandService.findUserByEmail(request.email)
        
        if (existingUser != null) {
            // Send "account exists" email
            emailService.sendAccountExistsNotification(request.email)
        } else {
            // Create new user
            val result = commandService.createUser(request.toCommand())
            
            when (result) {
                is Result.Success -> {
                    emailService.sendWelcomeEmail(result.value)
                }
                is Result.Failure -> {
                    // Log error but don't reveal to client
                    Log.errorf("User registration failed: %s", result.error)
                }
            }
        }
        
        // Always return same response
        return Response
            .status(Response.Status.ACCEPTED)
            .entity(mapOf(
                "message" to "Registration received. Check your email for next steps."
            ))
            .build()
    }
}
```

**Estimated Effort:** 2 days
**Dependencies:** Email service integration
**Testing:** 
- Test both existing and new user flows
- Verify same response timing

### 7.2 Phase 2: Error Infrastructure (Week 3-4)

**Priority: P1 - Important for Operations**

#### Task 2.1: Implement Error Classification

**File:** `platform-shared/common-types/src/main/kotlin/com.erp.shared.types/errors/ErrorClassifier.kt`

```kotlin
package com.erp.shared.types.errors

data class ClassifiedError(
    val code: String,
    val message: String,
    val category: ErrorCategory,
    val severity: ErrorSeverity,
    val isRetryable: Boolean,
    val userFacingMessage: String
)

object ErrorClassifier {
    fun classify(code: String): ClassifiedError {
        return ERROR_CLASSIFICATIONS[code] ?: DEFAULT_CLASSIFICATION
    }
    
    private val ERROR_CLASSIFICATIONS = mapOf(
        // Validation errors
        "WEAK_PASSWORD" to ClassifiedError(
            code = "WEAK_PASSWORD",
            message = "Password does not meet requirements",
            category = ErrorCategory.VALIDATION,
            severity = ErrorSeverity.INFO,
            isRetryable = true,
            userFacingMessage = "Your password doesn't meet our security requirements."
        ),
        
        // Authentication errors
        "INVALID_CREDENTIALS" to ClassifiedError(
            code = "INVALID_CREDENTIALS",
            message = "Invalid credentials",
            category = ErrorCategory.AUTHENTICATION,
            severity = ErrorSeverity.WARNING,
            isRetryable = true,
            userFacingMessage = "The email or password is incorrect."
        ),
        
        "ACCOUNT_LOCKED" to ClassifiedError(
            code = "ACCOUNT_LOCKED",
            message = "Account locked",
            category = ErrorCategory.AUTHENTICATION,
            severity = ErrorSeverity.WARNING,
            isRetryable = false,
            userFacingMessage = "Your account is temporarily locked for security reasons."
        ),
        
        // Authorization errors
        "INSUFFICIENT_PERMISSIONS" to ClassifiedError(
            code = "INSUFFICIENT_PERMISSIONS",
            message = "Insufficient permissions",
            category = ErrorCategory.AUTHORIZATION,
            severity = ErrorSeverity.WARNING,
            isRetryable = false,
            userFacingMessage = "You don't have permission to perform this action."
        ),
        
        // Resource errors
        "ROLE_NOT_FOUND" to ClassifiedError(
            code = "ROLE_NOT_FOUND",
            message = "Role not found",
            category = ErrorCategory.RESOURCE_NOT_FOUND,
            severity = ErrorSeverity.INFO,
            isRetryable = false,
            userFacingMessage = "That role doesn't exist."
        ),
        
        // Conflict errors
        "ROLE_NAME_EXISTS" to ClassifiedError(
            code = "ROLE_NAME_EXISTS",
            message = "Role name already exists",
            category = ErrorCategory.CONFLICT,
            severity = ErrorSeverity.INFO,
            isRetryable = true,
            userFacingMessage = "A role with that name already exists."
        ),
        
        // Business rule errors
        "ROLE_IMMUTABLE" to ClassifiedError(
            code = "ROLE_IMMUTABLE",
            message = "System role cannot be modified",
            category = ErrorCategory.BUSINESS_RULE,
            severity = ErrorSeverity.INFO,
            isRetryable = false,
            userFacingMessage = "This role cannot be modified because it's a system role."
        ),
        
        // Infrastructure errors
        "DATABASE_CONNECTION_FAILED" to ClassifiedError(
            code = "DATABASE_CONNECTION_FAILED",
            message = "Database connection failed",
            category = ErrorCategory.DATABASE,
            severity = ErrorSeverity.CRITICAL,
            isRetryable = true,
            userFacingMessage = "We're experiencing technical difficulties. Please try again later."
        )
    )
    
    private val DEFAULT_CLASSIFICATION = ClassifiedError(
        code = "UNKNOWN_ERROR",
        message = "An unknown error occurred",
        category = ErrorCategory.SYSTEM,
        severity = ErrorSeverity.ERROR,
        isRetryable = false,
        userFacingMessage = "Something went wrong. Please try again or contact support."
    )
}
```

**Estimated Effort:** 2 days
**Dependencies:** None
**Testing:** Unit tests for all known error codes

#### Task 2.2: Structured Logging with Error Context

**File:** `platform-shared/common-types/src/main/kotlin/com.erp.shared.types/logging/ErrorLogger.kt`

```kotlin
package com.erp.shared.types.logging

import com.erp.shared.types.errors.ErrorCategory
import com.erp.shared.types.errors.ErrorSeverity
import com.erp.shared.types.results.DomainError
import io.quarkus.logging.Log
import org.jboss.logging.MDC
import java.time.Instant

object ErrorLogger {
    fun logError(
        error: DomainError,
        category: ErrorCategory,
        severity: ErrorSeverity,
        operation: String,
        additionalContext: Map<String, Any> = emptyMap()
    ) {
        val errorId = java.util.UUID.randomUUID().toString()
        val traceId = MDC.get("traceId") ?: "no-trace"
        
        val context = buildMap {
            put("errorId", errorId)
            put("errorCode", error.code)
            put("errorCategory", category.name)
            put("errorSeverity", severity.name)
            put("operation", operation)
            put("timestamp", Instant.now().toString())
            put("traceId", traceId)
            
            MDC.get("tenantId")?.let { put("tenantId", it) }
            MDC.get("userId")?.let { put("userId", it) }
            
            // Add masked details
            error.details.forEach { (key, value) ->
                put(key, maskSensitiveValue(key, value))
            }
            
            putAll(additionalContext)
        }
        
        when (severity) {
            ErrorSeverity.DEBUG -> Log.debug(formatLogMessage(error, context))
            ErrorSeverity.INFO -> Log.info(formatLogMessage(error, context))
            ErrorSeverity.WARNING -> Log.warn(formatLogMessage(error, context))
            ErrorSeverity.ERROR -> Log.error(formatLogMessage(error, context), error.cause)
            ErrorSeverity.CRITICAL -> Log.fatal(formatLogMessage(error, context), error.cause)
        }
        
        // Store errorId in MDC for response correlation
        MDC.put("errorId", errorId)
    }
    
    private fun formatLogMessage(error: DomainError, context: Map<String, Any>): String {
        return buildString {
            append("[${context["errorSeverity"]}] ")
            append("[${context["errorCode"]}] ")
            append("${error.message} | ")
            append("operation=${context["operation"]} | ")
            append("errorId=${context["errorId"]} | ")
            append("traceId=${context["traceId"]} | ")
            
            context.filterKeys { 
                it !in setOf("errorSeverity", "errorCode", "operation", "errorId", "traceId", "timestamp") 
            }.forEach { (key, value) ->
                append("$key=$value | ")
            }
        }
    }
    
    private fun maskSensitiveValue(key: String, value: String): String {
        return when {
            key.contains("password", ignoreCase = true) -> "***"
            key.contains("token", ignoreCase = true) -> "***"
            key.contains("secret", ignoreCase = true) -> "***"
            key.contains("email", ignoreCase = true) -> maskEmail(value)
            key.contains("ssn", ignoreCase = true) -> "***-**-${value.takeLast(4)}"
            else -> value
        }
    }
    
    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***@***"
        val local = parts[0]
        val masked = if (local.length <= 2) "*".repeat(local.length)
                    else local.take(2) + "*".repeat(local.length - 2)
        return "$masked@${parts[1]}"
    }
}

enum class ErrorSeverity {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

enum class ErrorCategory {
    VALIDATION,
    AUTHENTICATION,
    AUTHORIZATION,
    RESOURCE_NOT_FOUND,
    CONFLICT,
    RATE_LIMIT,
    BUSINESS_RULE,
    EXTERNAL_SERVICE,
    DATABASE,
    SYSTEM
}
```

**Estimated Effort:** 2 days
**Dependencies:** Task 2.1
**Testing:** Unit tests for masking, integration tests for logging

#### Task 2.3: Centralized Error Response Factory

**File:** `identity-infrastructure/adapter/input/rest/ErrorResponseFactory.kt`

```kotlin
package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.shared.types.errors.Environment
import com.erp.shared.types.errors.ErrorClassifier
import com.erp.shared.types.results.DomainError
import org.jboss.logging.MDC
import java.time.Instant

object ErrorResponseFactory {
    
    fun createErrorResponse(
        error: DomainError,
        environment: Environment = Environment.PRODUCTION
    ): ErrorResponse {
        val classified = ErrorClassifier.classify(error.code)
        val errorId = MDC.get("errorId") ?: java.util.UUID.randomUUID().toString()
        
        return when (environment) {
            Environment.PRODUCTION -> ErrorResponse(
                message = classified.userFacingMessage,
                errorId = errorId,
                timestamp = Instant.now().toString(),
                suggestions = getSuggestions(error.code),
                actions = getActions(error.code)
            )
            
            Environment.STAGING -> ErrorResponse(
                message = classified.userFacingMessage,
                errorId = errorId,
                timestamp = Instant.now().toString(),
                code = error.code,  // Include code in staging
                suggestions = getSuggestions(error.code)
            )
            
            Environment.DEVELOPMENT -> ErrorResponse(
                message = error.message,
                errorId = errorId,
                timestamp = Instant.now().toString(),
                code = error.code,
                details = error.details,  // Full details in dev
                category = classified.category.name,
                severity = classified.severity.name
            )
        }
    }
    
    private fun getSuggestions(errorCode: String): List<String>? {
        return SUGGESTIONS[errorCode]
    }
    
    private fun getActions(errorCode: String): List<ErrorAction>? {
        return ACTIONS[errorCode]
    }
    
    private val SUGGESTIONS = mapOf(
        "INVALID_CREDENTIALS" to listOf(
            "Double-check your email and password",
            "Use 'Forgot Password' if needed",
            "Contact support if the problem persists"
        ),
        "ACCOUNT_LOCKED" to listOf(
            "Your account will automatically unlock in 30 minutes",
            "Contact support for immediate assistance"
        ),
        "WEAK_PASSWORD" to listOf(
            "Use at least 12 characters",
            "Include uppercase, lowercase, numbers, and special characters"
        ),
        "ROLE_IMMUTABLE" to listOf(
            "Create a new custom role instead",
            "Copy permissions from this role to a new one"
        )
    )
    
    private val ACTIONS = mapOf(
        "INVALID_CREDENTIALS" to listOf(
            ErrorAction("Forgot Password?", "/auth/forgot-password")
        ),
        "ACCOUNT_LOCKED" to listOf(
            ErrorAction("Contact Support", "/support/unlock-account")
        ),
        "ROLE_IMMUTABLE" to listOf(
            ErrorAction("Create New Role", "/roles/create")
        )
    )
}

data class ErrorResponse(
    val message: String,
    val errorId: String,
    val timestamp: String,
    val code: String? = null,
    val details: Map<String, String>? = null,
    val category: String? = null,
    val severity: String? = null,
    val suggestions: List<String>? = null,
    val actions: List<ErrorAction>? = null
)

data class ErrorAction(
    val label: String,
    val url: String,
    val method: String = "GET"
)
```

**Estimated Effort:** 1 day
**Dependencies:** Task 2.1, Task 2.2
**Testing:** Unit tests for all environments

### 7.3 Phase 3: Observability & Monitoring (Week 5-6)

**Priority: P1 - Important for Production Operations**

#### Task 3.1: Error Metrics Collection

**File:** `platform-shared/common-types/src/main/kotlin/com.erp.shared.types/metrics/ErrorMetrics.kt`

```kotlin
package com.erp.shared.types.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class ErrorMetrics @Inject constructor(
    private val registry: MeterRegistry
) {
    fun recordError(
        errorCode: String,
        category: String,
        severity: String,
        operation: String
    ) {
        Counter.builder("errors.total")
            .tag("code", errorCode)
            .tag("category", category)
            .tag("severity", severity)
            .tag("operation", operation)
            .register(registry)
            .increment()
    }
    
    fun recordAuthenticationFailure(reason: String) {
        Counter.builder("authentication.failures")
            .tag("reason", reason)
            .register(registry)
            .increment()
    }
    
    fun recordValidationError(field: String, code: String) {
        Counter.builder("validation.errors")
            .tag("field", field)
            .tag("code", code)
            .register(registry)
            .increment()
    }
    
    fun recordRetry(operation: String, attempt: Int, success: Boolean) {
        Counter.builder("operation.retries")
            .tag("operation", operation)
            .tag("attempt", attempt.toString())
            .tag("success", success.toString())
            .register(registry)
            .increment()
    }
}
```

**Estimated Effort:** 2 days
**Dependencies:** Micrometer/Prometheus setup
**Testing:** Integration tests with metric assertions

#### Task 3.2: Distributed Tracing Integration

**File:** `platform-shared/common-types/src/main/kotlin/com.erp.shared.types/tracing/ErrorTracing.kt`

```kotlin
package com.erp.shared.types.tracing

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import com.erp.shared.types.results.DomainError

object ErrorTracing {
    fun recordError(error: DomainError, operation: String) {
        val span = Span.current()
        
        span.setStatus(StatusCode.ERROR, error.message)
        span.setAttribute("error.code", error.code)
        span.setAttribute("error.message", error.message)
        span.setAttribute("operation", operation)
        
        error.details.forEach { (key, value) =>
            span.setAttribute("error.detail.$key", value)
        }
        
        error.cause?.let {
            span.recordException(it)
        }
    }
    
    fun recordValidationErrors(errors: List<com.erp.shared.types.results.ValidationError>) {
        val span = Span.current()
        
        span.setAttribute("validation.error.count", errors.size.toLong())
        errors.forEachIndexed { index, error ->
            span.setAttribute("validation.error.$index.field", error.field)
            span.setAttribute("validation.error.$index.code", error.code)
        }
    }
}
```

**Estimated Effort:** 1 day
**Dependencies:** OpenTelemetry setup
**Testing:** Trace verification tests

### 7.4 Phase 4: Resilience Patterns (Week 7-8)

**Priority: P2 - Enhanced Reliability**

#### Task 4.1: Retry Policy for Transient Failures

**File:** `platform-shared/common-types/src/main/kotlin/com.erp.shared.types/resilience/RetryPolicy.kt`

```kotlin
package com.erp.shared.types.resilience

import com.erp.shared.types.results.Result
import kotlinx.coroutines.delay
import kotlin.math.pow

class RetryPolicy(
    val maxAttempts: Int = 3,
    val baseDelayMs: Long = 100,
    val maxDelayMs: Long = 5000,
    val retryableErrors: Set<String> = DEFAULT_RETRYABLE_ERRORS
) {
    suspend fun <T> execute(
        operation: String,
        block: suspend (attempt: Int) -> Result<T>
    ): Result<T> {
        var lastError: com.erp.shared.types.results.DomainError? = null
        
        repeat(maxAttempts) { attempt ->
            when (val result = block(attempt + 1)) {
                is Result.Success -> return result
                is Result.Failure -> {
                    lastError = result.error
                    
                    if (!isRetryable(result.error.code)) {
                        return result
                    }
                    
                    if (attempt < maxAttempts - 1) {
                        val delayMs = calculateDelay(attempt)
                        delay(delayMs)
                    }
                }
            }
        }
        
        return Result.failure(
            code = "MAX_RETRIES_EXCEEDED",
            message = "Operation failed after $maxAttempts attempts",
            cause = lastError
        )
    }
    
    private fun isRetryable(errorCode: String): Boolean {
        return errorCode in retryableErrors
    }
    
    private fun calculateDelay(attempt: Int): Long {
        val exponentialDelay = baseDelayMs * 2.0.pow(attempt).toLong()
        return minOf(exponentialDelay, maxDelayMs)
    }
    
    companion object {
        val DEFAULT_RETRYABLE_ERRORS = setOf(
            "DATABASE_CONNECTION_FAILED",
            "DATABASE_TIMEOUT",
            "EXTERNAL_SERVICE_UNAVAILABLE",
            "RATE_LIMIT_EXCEEDED"
        )
    }
}
```

**Estimated Effort:** 3 days
**Dependencies:** None
**Testing:** Retry behavior tests, timing tests

#### Task 4.2: Circuit Breaker for External Dependencies

**File:** `platform-shared/common-types/src/main/kotlin/com.erp.shared.types/resilience/CircuitBreaker.kt`

```kotlin
package com.erp.shared.types.resilience

import com.erp.shared.types.results.Result
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class CircuitBreaker(
    val failureThreshold: Int = 5,
    val resetTimeout: Duration = Duration.ofMinutes(1),
    val halfOpenMaxCalls: Int = 3
) {
    private val state = AtomicReference(State.CLOSED)
    private val failureCount = AtomicInteger(0)
    private val lastFailureTime = AtomicReference<Instant?>(null)
    private val halfOpenCalls = AtomicInteger(0)
    
    suspend fun <T> execute(block: suspend () -> Result<T>): Result<T> {
        when (val currentState = state.get()) {
            State.OPEN -> {
                val lastFailure = lastFailureTime.get()
                if (lastFailure != null && 
                    Duration.between(lastFailure, Instant.now()) >= resetTimeout) {
                    state.set(State.HALF_OPEN)
                    halfOpenCalls.set(0)
                } else {
                    return Result.failure(
                        code = "CIRCUIT_BREAKER_OPEN",
                        message = "Circuit breaker is open"
                    )
                }
            }
            
            State.HALF_OPEN -> {
                if (halfOpenCalls.incrementAndGet() > halfOpenMaxCalls) {
                    return Result.failure(
                        code = "CIRCUIT_BREAKER_HALF_OPEN",
                        message = "Circuit breaker is half-open, max calls exceeded"
                    )
                }
            }
            
            State.CLOSED -> {
                // Normal operation
            }
        }
        
        return try {
            val result = block()
            
            when (result) {
                is Result.Success -> {
                    onSuccess()
                    result
                }
                is Result.Failure -> {
                    onFailure()
                    result
                }
            }
        } catch (e: Exception) {
            onFailure()
            Result.failure(
                code = "CIRCUIT_BREAKER_ERROR",
                message = "Circuit breaker caught exception",
                cause = e
            )
        }
    }
    
    private fun onSuccess() {
        when (state.get()) {
            State.HALF_OPEN -> {
                state.set(State.CLOSED)
                failureCount.set(0)
            }
            State.CLOSED -> {
                failureCount.set(0)
            }
            State.OPEN -> {
                // Should not happen
            }
        }
    }
    
    private fun onFailure() {
        lastFailureTime.set(Instant.now())
        val failures = failureCount.incrementAndGet()
        
        when (state.get()) {
            State.HALF_OPEN -> {
                state.set(State.OPEN)
            }
            State.CLOSED -> {
                if (failures >= failureThreshold) {
                    state.set(State.OPEN)
                }
            }
            State.OPEN -> {
                // Already open
            }
        }
    }
    
    enum class State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}
```

**Estimated Effort:** 3 days
**Dependencies:** None
**Testing:** State transition tests, concurrent access tests

---

## 8. Code Examples & Patterns

### 8.1 Complete Error Handling Flow

**Example: User Creation with Full Error Handling**

```kotlin
// Application Service Layer
@ApplicationScoped
class IdentityCommandService(
    private val userRepository: UserRepository,
    private val retryPolicy: RetryPolicy,
    private val metrics: ErrorMetrics,
    private val errorLogger: ErrorLogger
) {
    suspend fun createUser(command: CreateUserCommand): Result<User> {
        val traceId = MDC.get("traceId") ?: UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()
        
        return retryPolicy.execute("createUser") { attempt ->
            val result = userRepository.save(command.toUser())
            
            when (result) {
                is Result.Success -> {
                    metrics.recordSuccess("createUser", System.currentTimeMillis() - startTime)
                    Log.infof("[%s] ✓ User created successfully", traceId)
                    result
                }
                is Result.Failure -> {
                    val classified = ErrorClassifier.classify(result.error.code)
                    
                    errorLogger.logError(
                        error = result.error,
                        category = classified.category,
                        severity = classified.severity,
                        operation = "createUser",
                        additionalContext = mapOf(
                            "attempt" to attempt,
                            "durationMs" to (System.currentTimeMillis() - startTime)
                        )
                    )
                    
                    metrics.recordError(
                        errorCode = result.error.code,
                        category = classified.category.name,
                        severity = classified.severity.name,
                        operation = "createUser"
                    )
                    
                    ErrorTracing.recordError(result.error, "createUser")
                    
                    result
                }
            }
        }
    }
}

// REST Resource Layer
@Path("/api/v1/users")
class UserResource(
    private val commandService: IdentityCommandService,
    private val environment: Environment
) {
    @POST
    suspend fun createUser(request: CreateUserRequest): Response {
        val result = commandService.createUser(request.toCommand())
        
        return when (result) {
            is Result.Success -> Response
                .status(Response.Status.CREATED)
                .entity(result.value.toDTO())
                .build()
                
            is Result.Failure -> {
                val errorResponse = ErrorResponseFactory.createErrorResponse(
                    error = result.error,
                    environment = environment
                )
                
                Response
                    .status(mapHttpStatus(result.error.code))
                    .entity(errorResponse)
                    .build()
            }
        }
    }
}
```

### 8.2 Validation Error Patterns

**Example: Password Validation with Detailed Feedback**

```kotlin
class PasswordValidator(private val policy: PasswordPolicy) {
    fun validate(password: String): Result<Unit> {
        val errors = mutableListOf<ValidationError>()
        
        if (password.length < policy.minLength) {
            errors.add(ValidationError(
                field = "password",
                code = "TOO_SHORT",
                message = "Password must be at least ${policy.minLength} characters",
                rejectedValue = password.length.toString()
            ))
        }
        
        if (!password.any { it.isUpperCase() }) {
            errors.add(ValidationError(
                field = "password",
                code = "MISSING_UPPERCASE",
                message = "Password must contain at least one uppercase letter"
            ))
        }
        
        if (!password.any { it.isLowerCase() }) {
            errors.add(ValidationError(
                field = "password",
                code = "MISSING_LOWERCASE",
                message = "Password must contain at least one lowercase letter"
            ))
        }
        
        if (!password.any { it.isDigit() }) {
            errors.add(ValidationError(
                field = "password",
                code = "MISSING_DIGIT",
                message = "Password must contain at least one number"
            ))
        }
        
        if (!password.any { it in policy.specialCharacters }) {
            errors.add(ValidationError(
                field = "password",
                code = "MISSING_SPECIAL",
                message = "Password must contain at least one special character"
            ))
        }
        
        return if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(
                code = "WEAK_PASSWORD",
                message = "Password does not meet security requirements",
                validationErrors = errors
            )
        }
    }
}
```

### 8.3 Security-First Authentication Pattern

```kotlin
class SecureAuthenticationService(
    private val userRepository: UserRepository,
    private val passwordVerifier: PasswordVerifier,
    private val securityMetrics: SecurityMetrics
) {
    companion object {
        private const val MIN_AUTH_TIME_MS = 100L
        private val GENERIC_ERROR = DomainError(
            code = "AUTHENTICATION_FAILED",
            message = "Authentication failed"
        )
    }
    
    suspend fun authenticate(
        email: String,
        password: String
    ): Result<AuthenticationToken> {
        val startTime = System.currentTimeMillis()
        
        // Always fetch or create dummy
        val user = userRepository.findByEmail(email) ?: User.createDummy(email)
        
        // Always verify (constant time)
        val verified = passwordVerifier.verify(password, user.passwordHash)
        
        // Ensure minimum delay
        ensureMinimumDuration(startTime, MIN_AUTH_TIME_MS)
        
        return if (verified && user.isReal && user.canLogin()) {
            val token = generateToken(user)
            securityMetrics.recordSuccessfulAuth(email)
            Result.success(token)
        } else {
            if (user.isReal) {
                userRepository.recordFailedLogin(user.id)
                securityMetrics.recordFailedAuth(email, "invalid_credentials")
            }
            Result.failure(GENERIC_ERROR)
        }
    }
    
    private suspend fun ensureMinimumDuration(startMs: Long, minMs: Long) {
        val elapsed = System.currentTimeMillis() - startMs
        if (elapsed < minMs) {
            delay(minMs - elapsed)
        }
    }
}
```

---

## 9. Monitoring & Alerting

### 9.1 Error Metrics Dashboard

**Prometheus Queries:**
```promql
# Error rate by category
rate(errors_total[5m])

# Error rate by severity
sum(rate(errors_total{severity="CRITICAL"}[5m])) by (operation)

# Authentication failure rate
rate(authentication_failures_total[5m])

# Top 10 error codes
topk(10, sum(rate(errors_total[5m])) by (code))

# Circuit breaker state
circuit_breaker_state{service="identity"}
```

**Alert Rules:**
```yaml
groups:
  - name: error_handling
    rules:
      # Critical errors
      - alert: HighCriticalErrorRate
        expr: rate(errors_total{severity="CRITICAL"}[5m]) > 0.1
        for: 1m
        annotations:
          summary: "High critical error rate detected"
          
      # Authentication failures
      - alert: SuspiciousAuthActivity
        expr: rate(authentication_failures_total[5m]) > 5
        for: 2m
        annotations:
          summary: "Potential brute force attack"
          
      # Circuit breaker open
      - alert: CircuitBreakerOpen
        expr: circuit_breaker_state == 1
        for: 5m
        annotations:
          summary: "Circuit breaker open for {{ $labels.service }}"
```

### 9.2 Log Aggregation Queries

**Example Queries for ELK/Splunk:**

```
# Find all authentication failures in last hour
errorCode:INVALID_CREDENTIALS AND timestamp:[now-1h TO now]

# Find errors for specific user
userId:"123e4567-e89b-12d3-a456-426614174000" AND errorSeverity:ERROR

# Find all enumeration attempts
(errorCode:USERNAME_IN_USE OR errorCode:EMAIL_IN_USE) 
  AND timestamp:[now-1d TO now]
  | stats count by sourceIP

# Trace error flow
traceId:"550e8400-e29b-41d4-a716-446655440000"
```

### 9.3 Security Event Monitoring

**Security-Critical Events to Monitor:**

| Event | Threshold | Action |
|-------|-----------|--------|
| Multiple failed logins (same user) | 5 in 15 min | Lock account |
| Multiple failed logins (same IP) | 10 in 15 min | Rate limit IP |
| Account enumeration attempts | 20 in 1 hour | Block IP |
| Unusual error patterns | 100 errors/min | Page on-call |
| Critical errors | Any occurrence | Immediate alert |

---

## 10. Appendices

### Appendix A: Error Code Reference

**Complete Error Code Catalog:**

| Code | Category | Severity | HTTP Status | Retryable | User Message |
|------|----------|----------|-------------|-----------|--------------|
| `WEAK_PASSWORD` | Validation | INFO | 422 | Yes | Password doesn't meet requirements |
| `INVALID_CREDENTIALS` | Authentication | WARNING | 401 | Yes | Email or password incorrect |
| `ACCOUNT_LOCKED` | Authentication | WARNING | 403 | No | Account temporarily locked |
| `INSUFFICIENT_PERMISSIONS` | Authorization | WARNING | 403 | No | No permission for this action |
| `USER_NOT_FOUND` | NotFound | INFO | 404 | No | Resource not found |
| `TENANT_NOT_FOUND` | NotFound | INFO | 404 | No | Resource not found |
| `ROLE_NOT_FOUND` | NotFound | INFO | 404 | No | Role doesn't exist |
| `USERNAME_IN_USE` | Conflict | INFO | 409 | Yes | Registration submitted |
| `EMAIL_IN_USE` | Conflict | INFO | 409 | Yes | Registration submitted |
| `ROLE_NAME_EXISTS` | Conflict | INFO | 409 | Yes | Role name already exists |
| `ROLE_IMMUTABLE` | BusinessRule | INFO | 400 | No | System role cannot be modified |
| `DATABASE_ERROR` | Database | CRITICAL | 500 | Yes | Technical difficulties |

### Appendix B: Migration Checklist

**Pre-Deployment Checklist:**
- [ ] Error sanitizer implemented
- [ ] All endpoints updated to use sanitizer
- [ ] Environment configuration added
- [ ] Anti-enumeration patterns deployed
- [ ] Constant-time authentication implemented
- [ ] Rate limiting configured
- [ ] Metrics collection enabled
- [ ] Alert rules configured
- [ ] Documentation updated
- [ ] Team trained on new policies

### Appendix C: Performance Impact Analysis

**Expected Performance Impact:**

| Change | Impact | Mitigation |
|--------|--------|------------|
| Constant-time auth | +100ms per auth | Acceptable security cost |
| Error classification | <1ms | Cached lookups |
| Metrics collection | <0.5ms | Async recording |
| Structured logging | +2-5ms | Async appenders |
| Retry policies | Varies | Circuit breakers prevent cascading |

### Appendix D: References

**Standards & Guidelines:**
- OWASP ASVS 4.0 - Application Security Verification Standard
- NIST SP 800-53 Rev. 5 - Security and Privacy Controls
- RFC 7807 - Problem Details for HTTP APIs
- CWE-209 - Generation of Error Message Containing Sensitive Information
- CWE-203 - Observable Discrepancy (Timing Attacks)

**Industry Best Practices:**
- AWS Error Handling Best Practices
- Google Cloud API Design Guide
- Microsoft REST API Guidelines
- Stripe API Error Handling
- GitHub API Error Responses

---

## Summary & Next Steps

### Executive Recommendations

1. **Immediate Actions (P0):**
   - Deploy error sanitization in production
   - Implement anti-enumeration for registration
   - Add rate limiting to authentication endpoints
   
2. **Short Term (P1):**
   - Complete error classification system
   - Deploy structured logging
   - Set up monitoring dashboards
   
3. **Medium Term (P2):**
   - Implement retry policies
   - Deploy circuit breakers
   - Add comprehensive alerting

### Success Criteria

- **Security:** Zero information leakage incidents
- **User Experience:** <2% error-related support tickets
- **Observability:** 100% error traceability
- **Reliability:** <0.01% error rate for non-user errors

### Review & Iteration

This policy should be reviewed:
- Quarterly for effectiveness
- After security incidents
- When adding new features
- Based on user feedback

---

**Document Version History:**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-11-07 | Senior Engineering Team | Initial comprehensive policy |

---

**Approval:**

- [ ] Security Team Review
- [ ] Engineering Lead Approval
- [ ] Product Team Review
- [ ] Compliance Review

---

*This document represents best practices for error handling and should be treated as a living document, updated as the platform evolves and new security threats emerge.*

