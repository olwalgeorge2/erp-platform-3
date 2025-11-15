package com.erp.finance.accounting.infrastructure.adapter.input.rest

import com.erp.finance.accounting.application.service.DimensionValidationException
import com.erp.finance.accounting.domain.model.DimensionType
import com.erp.financial.shared.validation.FinanceValidationErrorCode
import com.erp.financial.shared.validation.FinanceValidationException
import com.erp.financial.shared.validation.ValidationMessageResolver
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun parseDimensionType(
    raw: String,
    locale: Locale,
): DimensionType {
    val normalized = raw.replace('-', '_').uppercase(Locale.getDefault())
    return try {
        DimensionType.valueOf(normalized)
    } catch (ex: IllegalArgumentException) {
        val valid =
            DimensionType
                .entries
                .joinToString()
        throw FinanceValidationException(
            errorCode = FinanceValidationErrorCode.FINANCE_INVALID_DIMENSION_TYPE,
            field = "dimensionType",
            rejectedValue = raw,
            locale = locale,
            message =
                ValidationMessageResolver.resolve(
                    FinanceValidationErrorCode.FINANCE_INVALID_DIMENSION_TYPE,
                    locale,
                    raw,
                    valid,
                ),
        )
    }
}

internal fun DimensionValidationException.toFinanceValidationException(locale: Locale): FinanceValidationException {
    val code: FinanceValidationErrorCode
    val args: Array<Any?>
    when (reason) {
        DimensionValidationException.Reason.NOT_FOUND -> {
            code = FinanceValidationErrorCode.FINANCE_DIMENSION_NOT_FOUND
            args = arrayOf(dimensionType.displayName(), dimensionId?.toString() ?: "unknown")
        }
        DimensionValidationException.Reason.INACTIVE -> {
            code = FinanceValidationErrorCode.FINANCE_DIMENSION_INACTIVE
            args =
                arrayOf(
                    dimensionType.displayName(),
                    dimensionId?.toString() ?: "unknown",
                    bookingDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "",
                )
        }
        DimensionValidationException.Reason.MANDATORY_MISSING -> {
            code = FinanceValidationErrorCode.FINANCE_MANDATORY_DIMENSION_MISSING
            args = arrayOf(dimensionType.displayName(), accountType.name)
        }
    }
    val fieldName = "dimensions.${dimensionType.name.lowercase(Locale.getDefault())}"
    val message = ValidationMessageResolver.resolve(code, locale, *args)
    val rejectedValue = dimensionId?.toString()
    return FinanceValidationException(
        errorCode = code,
        field = fieldName,
        rejectedValue = rejectedValue,
        locale = locale,
        message = message,
    )
}

private fun DimensionType.displayName(): String = name.replace('_', ' ').lowercase(Locale.getDefault())
