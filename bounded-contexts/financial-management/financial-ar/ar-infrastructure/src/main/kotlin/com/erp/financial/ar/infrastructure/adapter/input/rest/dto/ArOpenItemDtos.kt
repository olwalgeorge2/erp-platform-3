package com.erp.financial.ar.infrastructure.adapter.input.rest.dto

import com.erp.financial.ar.application.port.input.query.ArAgingBucket
import com.erp.financial.ar.application.port.input.query.ArAgingDetailLine
import com.erp.financial.ar.application.port.input.query.ArAgingDetailResult
import com.erp.financial.ar.application.port.input.query.ArAgingQuery
import com.erp.financial.ar.application.port.input.query.ArAgingSummaryLine
import com.erp.financial.ar.application.port.input.query.ArAgingSummaryResult
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.QueryParam
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID

data class ArAgingRequest(
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
    @field:QueryParam("companyCodeId")
    var companyCodeId: UUID? = null,
    @field:QueryParam("customerId")
    var customerId: UUID? = null,
    @field:NotBlank
    @field:Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "asOfDate must be formatted as yyyy-MM-dd")
    @field:QueryParam("asOfDate")
    var asOfDate: String? = null,
) {
    fun toQuery(): ArAgingQuery =
        ArAgingQuery(
            tenantId = tenantId ?: throw BadRequestException("tenantId is required"),
            companyCodeId = companyCodeId,
            customerId = customerId,
            asOfDate = parseAsOfDate(),
        )

    private fun parseAsOfDate(): LocalDate {
        val value = asOfDate ?: throw BadRequestException("asOfDate is required")
        return try {
            LocalDate.parse(value)
        } catch (ex: DateTimeParseException) {
            throw BadRequestException("Invalid asOfDate '$value'. Expected format yyyy-MM-dd.")
        }
    }
}

data class ArAgingDetailResponse(
    val asOfDate: LocalDate,
    val lines: List<ArAgingDetailLineResponse>,
)

data class ArAgingDetailLineResponse(
    val openItemId: UUID,
    val invoiceId: UUID,
    val customerId: UUID,
    val invoiceNumber: String,
    val dueDate: LocalDate,
    val bucket: ArAgingBucket,
    val amountMinor: Long,
    val currency: String,
)

data class ArAgingSummaryResponse(
    val asOfDate: LocalDate,
    val buckets: List<ArAgingSummaryLineResponse>,
)

data class ArAgingSummaryLineResponse(
    val bucket: ArAgingBucket,
    val totalAmountMinor: Long,
    val count: Long,
)

fun ArAgingDetailResult.toResponse(): ArAgingDetailResponse =
    ArAgingDetailResponse(
        asOfDate = asOfDate,
        lines = lines.map(ArAgingDetailLine::toResponse),
    )

fun ArAgingDetailLine.toResponse(): ArAgingDetailLineResponse =
    ArAgingDetailLineResponse(
        openItemId = openItemId,
        invoiceId = invoiceId,
        customerId = customerId,
        invoiceNumber = invoiceNumber,
        dueDate = dueDate,
        bucket = bucket,
        amountMinor = amountMinor,
        currency = currency,
    )

fun ArAgingSummaryResult.toResponse(): ArAgingSummaryResponse =
    ArAgingSummaryResponse(
        asOfDate = asOfDate,
        buckets = buckets.map(ArAgingSummaryLine::toResponse),
    )

fun ArAgingSummaryLine.toResponse(): ArAgingSummaryLineResponse =
    ArAgingSummaryLineResponse(
        bucket = bucket,
        totalAmountMinor = totalAmountMinor,
        count = count,
    )
