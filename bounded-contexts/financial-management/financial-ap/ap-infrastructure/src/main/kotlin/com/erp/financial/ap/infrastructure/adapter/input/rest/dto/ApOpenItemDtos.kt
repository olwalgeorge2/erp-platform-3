package com.erp.financial.ap.infrastructure.adapter.input.rest.dto

import com.erp.financial.ap.application.port.input.query.AgingBucket
import com.erp.financial.ap.application.port.input.query.AgingDetailLine
import com.erp.financial.ap.application.port.input.query.AgingDetailResult
import com.erp.financial.ap.application.port.input.query.AgingSummaryLine
import com.erp.financial.ap.application.port.input.query.AgingSummaryResult
import com.erp.financial.ap.application.port.input.query.ApAgingQuery
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.QueryParam
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID

data class AgingRequest(
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
    @field:QueryParam("companyCodeId")
    var companyCodeId: UUID? = null,
    @field:QueryParam("vendorId")
    var vendorId: UUID? = null,
    @field:NotBlank
    @field:Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "asOfDate must be formatted as yyyy-MM-dd")
    @field:QueryParam("asOfDate")
    var asOfDate: String? = null,
) {
    fun toQuery(): ApAgingQuery =
        ApAgingQuery(
            tenantId = tenantId ?: throw BadRequestException("tenantId is required"),
            companyCodeId = companyCodeId,
            vendorId = vendorId,
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

data class AgingDetailResponse(
    val asOfDate: LocalDate,
    val lines: List<AgingDetailLineResponse>,
)

data class AgingDetailLineResponse(
    val openItemId: UUID,
    val invoiceId: UUID,
    val vendorId: UUID,
    val invoiceNumber: String,
    val dueDate: LocalDate,
    val bucket: AgingBucket,
    val amountMinor: Long,
    val currency: String,
)

data class AgingSummaryResponse(
    val asOfDate: LocalDate,
    val buckets: List<AgingSummaryLineResponse>,
)

data class AgingSummaryLineResponse(
    val bucket: AgingBucket,
    val totalAmountMinor: Long,
    val count: Long,
)

fun AgingDetailResult.toResponse(): AgingDetailResponse =
    AgingDetailResponse(
        asOfDate = asOfDate,
        lines = lines.map(AgingDetailLine::toResponse),
    )

fun AgingDetailLine.toResponse(): AgingDetailLineResponse =
    AgingDetailLineResponse(
        openItemId = openItemId,
        invoiceId = invoiceId,
        vendorId = vendorId,
        invoiceNumber = invoiceNumber,
        dueDate = dueDate,
        bucket = bucket,
        amountMinor = amountMinor,
        currency = currency,
    )

fun AgingSummaryResult.toResponse(): AgingSummaryResponse =
    AgingSummaryResponse(
        asOfDate = asOfDate,
        buckets = buckets.map(AgingSummaryLine::toResponse),
    )

fun AgingSummaryLine.toResponse(): AgingSummaryLineResponse =
    AgingSummaryLineResponse(
        bucket = bucket,
        totalAmountMinor = totalAmountMinor,
        count = count,
    )
