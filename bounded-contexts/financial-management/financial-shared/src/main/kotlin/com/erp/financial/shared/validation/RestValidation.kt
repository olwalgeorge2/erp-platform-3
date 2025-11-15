package com.erp.financial.shared.validation

import jakarta.ws.rs.core.HttpHeaders
import java.util.Locale
import java.util.UUID

fun HttpHeaders?.preferredLocale(): Locale =
    this?.language
        ?: this?.acceptableLanguages?.firstOrNull()
        ?: Locale.getDefault()

fun parseUuidParam(
    raw: String,
    field: String,
    code: FinanceValidationErrorCode,
    locale: Locale,
): UUID =
    runCatching { UUID.fromString(raw) }
        .getOrElse {
            throw FinanceValidationException(
                errorCode = code,
                field = field,
                rejectedValue = raw,
                locale = locale,
                message = ValidationMessageResolver.resolve(code, locale, raw),
            )
        }
