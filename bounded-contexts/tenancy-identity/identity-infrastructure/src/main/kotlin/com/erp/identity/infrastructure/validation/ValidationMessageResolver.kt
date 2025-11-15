package com.erp.identity.infrastructure.validation

import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle
import java.util.concurrent.ConcurrentHashMap

object ValidationMessageResolver {
    private const val BUNDLE_NAME = "ValidationMessages"
    private val cache = ConcurrentHashMap<String, ResourceBundle>()

    fun resolve(
        code: ValidationErrorCode,
        locale: Locale,
        vararg args: Any?,
    ): String {
        val bundle = resourceBundle(locale)
        val pattern =
            if (bundle.containsKey(code.code)) {
                bundle.getString(code.code)
            } else {
                code.code
            }
        return MessageFormat(pattern, locale).format(args)
    }

    private fun resourceBundle(locale: Locale): ResourceBundle {
        val key = locale.toLanguageTag().ifBlank { locale.language }
        return cache.computeIfAbsent(key) { ResourceBundle.getBundle(BUNDLE_NAME, locale) }
    }
}
