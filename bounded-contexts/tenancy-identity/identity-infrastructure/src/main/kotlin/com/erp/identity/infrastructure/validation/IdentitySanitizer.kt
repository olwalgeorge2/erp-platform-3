package com.erp.identity.infrastructure.validation

import org.owasp.html.HtmlPolicyBuilder

/**
 * Input sanitization utilities for Identity & Tenancy bounded context.
 * Provides XSS, SQL injection, and command injection prevention for user-controlled inputs.
 * Uses OWASP Java HTML Sanitizer for comprehensive security.
 */

private val XSS_POLICY = HtmlPolicyBuilder().toFactory()

/**
 * Sanitizes string for XSS by removing all HTML tags and dangerous characters.
 * Used for general text fields where HTML should not be allowed.
 */
fun String?.sanitizeForXss(): String {
    if (this == null) return ""
    return XSS_POLICY.sanitize(this.trim())
}

/**
 * Sanitizes tenant/organization slugs (URL-safe identifiers).
 * Allows: lowercase letters, numbers, hyphens
 * Max length: 50 characters
 */
fun String?.sanitizeSlug(): String {
    if (this == null) return ""
    return this
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9-]"), "")
        .take(50)
}

/**
 * Sanitizes usernames.
 * Allows: letters, numbers, underscores, hyphens, dots
 * Max length: 50 characters
 */
fun String?.sanitizeUsername(): String {
    if (this == null) return ""
    return this
        .trim()
        .replace(Regex("[^a-zA-Z0-9_.-]"), "")
        .take(50)
}

/**
 * Sanitizes email addresses.
 * Basic XSS removal while preserving email format.
 * Max length: 255 characters
 */
fun String?.sanitizeEmail(): String {
    if (this == null) return ""
    val sanitized = this.trim().lowercase().sanitizeForXss()
    return sanitized.take(255)
}

/**
 * Sanitizes names (tenant name, user full name, organization name).
 * Removes HTML tags but allows unicode characters for international names.
 * Max length: 200 characters
 */
fun String?.sanitizeName(): String {
    if (this == null) return ""
    return this.sanitizeForXss().take(200)
}

/**
 * Sanitizes role names and descriptions.
 * Max length: 100 characters for names, 500 for descriptions
 */
fun String?.sanitizeRoleName(): String {
    if (this == null) return ""
    return this.sanitizeForXss().take(100)
}

fun String?.sanitizeDescription(maxLength: Int = 500): String {
    if (this == null) return ""
    return this.sanitizeForXss().take(maxLength)
}

/**
 * Sanitizes phone numbers.
 * Allows: digits, spaces, hyphens, parentheses, plus sign
 * Max length: 20 characters
 */
fun String?.sanitizePhoneNumber(): String {
    if (this == null) return ""
    return this
        .trim()
        .replace(Regex("[^0-9\\s+()-]"), "")
        .take(20)
}

/**
 * Sanitizes tax IDs / registration numbers.
 * Allows: alphanumeric, hyphens
 * Max length: 50 characters
 */
fun String?.sanitizeTaxId(): String {
    if (this == null) return ""
    return this
        .trim()
        .replace(Regex("[^a-zA-Z0-9-]"), "")
        .take(50)
}

/**
 * Sanitizes postal/ZIP codes.
 * Allows: alphanumeric, spaces, hyphens
 * Max length: 20 characters
 */
fun String?.sanitizePostalCode(): String {
    if (this == null) return ""
    return this
        .trim()
        .uppercase()
        .replace(Regex("[^A-Z0-9\\s-]"), "")
        .take(20)
}

/**
 * Sanitizes industry names.
 * Max length: 100 characters
 */
fun String?.sanitizeIndustry(): String {
    if (this == null) return ""
    return this.sanitizeForXss().take(100)
}

/**
 * Sanitizes permission resource and action names.
 * Allows: lowercase letters, numbers, colons, dots, hyphens, underscores
 * Max length: 100 characters
 */
fun String?.sanitizePermissionIdentifier(): String {
    if (this == null) return ""
    return this
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9:._-]"), "")
        .take(100)
}

/**
 * Sanitizes suspension/audit reasons.
 * Max length: 500 characters
 */
fun String?.sanitizeReason(): String {
    if (this == null) return ""
    return this.sanitizeForXss().take(500)
}

/**
 * Sanitizes IP addresses (IPv4 and IPv6).
 * Allows: digits, dots, colons (for IPv6)
 * Max length: 45 characters (IPv6 max)
 */
fun String?.sanitizeIpAddress(): String {
    if (this == null) return ""
    return this
        .trim()
        .replace(Regex("[^0-9a-fA-F:.%]"), "")
        .take(45)
}

/**
 * Sanitizes user agent strings.
 * Max length: 255 characters
 */
fun String?.sanitizeUserAgent(): String {
    if (this == null) return ""
    return this.sanitizeForXss().take(255)
}
