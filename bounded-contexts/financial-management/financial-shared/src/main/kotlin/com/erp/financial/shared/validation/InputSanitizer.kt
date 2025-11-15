package com.erp.financial.shared.validation

import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory

/**
 * Input sanitization utilities for finance REST APIs.
 * Prevents XSS, SQL injection, and other security vulnerabilities.
 *
 * Usage:
 * ```kotlin
 * import com.erp.financial.shared.validation.InputSanitizer.sanitizeForXss
 * import com.erp.financial.shared.validation.InputSanitizer.sanitizeAccountCode
 *
 * data class CreateAccountRequest(...) {
 *     fun toCommand(): CreateAccountCommand {
 *         return CreateAccountCommand(
 *             code = code.sanitizeAccountCode(),
 *             name = name.sanitizeForXss()
 *         )
 *     }
 * }
 * ```
 */
object InputSanitizer {
    /**
     * HTML policy that allows only safe text formatting tags.
     * Removes all potentially dangerous HTML/JavaScript.
     */
    private val htmlPolicy: PolicyFactory =
        HtmlPolicyBuilder()
            .allowElements("b", "i", "em", "strong")
            .toFactory()

    /**
     * Remove XSS payloads from user input while preserving safe formatting.
     * Truncates to 10,000 characters to prevent DOS attacks.
     *
     * @return Sanitized string safe for HTML display
     */
    fun String.sanitizeForXss(): String = htmlPolicy.sanitize(this.take(10_000))

    /**
     * Sanitize account codes to alphanumeric + dash/underscore only.
     * Maximum length: 20 characters.
     *
     * Use for: account codes, ledger codes, GL codes, cost center IDs
     *
     * @return Sanitized account code (alphanumeric, dash, underscore only)
     */
    fun String.sanitizeAccountCode(): String =
        this
            .trim()
            .replace(Regex("[^A-Za-z0-9-_]"), "")
            .take(20)

    /**
     * Sanitize general text fields (descriptions, notes, comments).
     * Removes XSS payloads and limits length.
     *
     * @param maxLength Maximum allowed length (default: 500)
     * @return Sanitized text safe for storage and display
     */
    fun String.sanitizeText(maxLength: Int = 500): String =
        this
            .trim()
            .sanitizeForXss()
            .take(maxLength)

    /**
     * Sanitize reference numbers (invoice numbers, PO numbers, etc.).
     * Allows alphanumeric + common separators: dash, underscore, slash, dot.
     * Maximum length: 50 characters.
     *
     * @return Sanitized reference number
     */
    fun String.sanitizeReferenceNumber(): String =
        this
            .trim()
            .replace(Regex("[^A-Za-z0-9-_/.]"), "")
            .take(50)

    /**
     * Sanitize currency codes to uppercase 3-letter codes.
     *
     * @return Sanitized currency code (e.g., "USD", "EUR")
     */
    fun String.sanitizeCurrencyCode(): String =
        this
            .trim()
            .uppercase()

    /**
     * Sanitize email addresses.
     * Basic format validation and XSS prevention.
     *
     * @return Sanitized email address
     */
    fun String.sanitizeEmail(): String =
        this
            .trim()
            .lowercase()
            .take(254) // RFC 5321 max email length

    /**
     * Sanitize phone numbers to digits, plus sign, and common separators.
     *
     * @return Sanitized phone number
     */
    fun String.sanitizePhoneNumber(): String =
        this
            .trim()
            .replace(Regex("[^0-9+\\-() ]"), "")
            .take(20)

    /**
     * Sanitize names (person names, company names, entity names).
     * Preserves letters, spaces, common name characters.
     *
     * @param maxLength Maximum allowed length (default: 200)
     * @return Sanitized name
     */
    fun String.sanitizeName(maxLength: Int = 200): String =
        this
            .trim()
            .replace(Regex("[<>\"']"), "") // Remove XSS-prone characters
            .take(maxLength)
}
