package com.erp.apigateway.validation

import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle
import java.util.concurrent.ConcurrentHashMap

object ValidationMessageResolver {
    private const val BUNDLE = "ValidationMessages"
    private val cache = ConcurrentHashMap<String, ResourceBundle>()

    fun resolve(
        code: GatewayValidationErrorCode,
        locale: Locale,
        vararg args: Any?,
    ): String {
        val bundle = ResourceBundle.getBundle(BUNDLE, locale)
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
        return cache.computeIfAbsent(key) { ResourceBundle.getBundle(BUNDLE, locale) }
    }
}
