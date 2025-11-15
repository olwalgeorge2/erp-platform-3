package com.erp.financial.ar.application.service

import com.erp.financial.ar.application.port.input.query.ArAgingBucket
import com.erp.financial.ar.application.port.input.query.ArAgingDetailLine
import com.erp.financial.ar.application.port.input.query.ArAgingDetailResult
import com.erp.financial.ar.application.port.input.query.ArAgingQuery
import com.erp.financial.ar.application.port.input.query.ArAgingSummaryLine
import com.erp.financial.ar.application.port.input.query.ArAgingSummaryResult
import com.erp.financial.ar.application.port.input.query.ArOpenItemQueryUseCase
import com.erp.financial.ar.application.port.output.ArOpenItemFilter
import com.erp.financial.ar.application.port.output.ArOpenItemRepository
import com.erp.financial.ar.domain.model.openitem.ArOpenItemStatus
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicLong

@ApplicationScoped
class ArOpenItemQueryService(
    private val repository: ArOpenItemRepository,
    meterRegistry: MeterRegistry,
) : ArOpenItemQueryUseCase {
    private val bucketGauges: Map<ArAgingBucket, AtomicLong> =
        ArAgingBucket
            .values()
            .associateWith { bucket ->
                meterRegistry.gauge(
                    "ar.open_items.amount_minor",
                    listOf(Tag.of("bucket", bucket.name.lowercase())),
                    AtomicLong(0),
                )!!
            }

    override fun getAgingDetail(query: ArAgingQuery): ArAgingDetailResult {
        val lines = computeLines(query)
        recordMetrics(lines)
        return ArAgingDetailResult(query.asOfDate, lines)
    }

    override fun getAgingSummary(query: ArAgingQuery): ArAgingSummaryResult {
        val lines = computeLines(query)
        recordMetrics(lines)
        val buckets =
            lines
                .groupBy { it.bucket }
                .map { (bucket, bucketLines) ->
                    ArAgingSummaryLine(
                        bucket = bucket,
                        totalAmountMinor = bucketLines.sumOf { it.amountMinor },
                        count = bucketLines.size.toLong(),
                    )
                }.sortedBy { it.bucket.ordinal }
        return ArAgingSummaryResult(query.asOfDate, buckets)
    }

    private fun computeLines(query: ArAgingQuery): List<ArAgingDetailLine> {
        val items =
            repository.list(
                ArOpenItemFilter(
                    tenantId = query.tenantId,
                    companyCodeId = query.companyCodeId,
                    customerId = query.customerId,
                    statuses = setOf(ArOpenItemStatus.OPEN, ArOpenItemStatus.PARTIALLY_PAID),
                ),
            )
        return items.map { item ->
            val bucket = determineBucket(item.dueDate, query.asOfDate)
            ArAgingDetailLine(
                openItemId = item.id,
                invoiceId = item.invoiceId,
                customerId = item.customerId,
                invoiceNumber = item.invoiceNumber,
                dueDate = item.dueDate,
                bucket = bucket,
                amountMinor = item.outstandingAmountMinor,
                currency = item.currency,
            )
        }
    }

    private fun determineBucket(
        dueDate: LocalDate,
        asOf: LocalDate,
    ): ArAgingBucket {
        val days = ChronoUnit.DAYS.between(dueDate, asOf)
        return when {
            days <= 0 -> ArAgingBucket.CURRENT
            days <= 30 -> ArAgingBucket.DAYS_1_30
            days <= 60 -> ArAgingBucket.DAYS_31_60
            days <= 90 -> ArAgingBucket.DAYS_61_90
            else -> ArAgingBucket.DAYS_91_PLUS
        }
    }

    private fun recordMetrics(lines: List<ArAgingDetailLine>) {
        val totals =
            lines
                .groupBy { it.bucket }
                .mapValues { (_, bucketLines) -> bucketLines.sumOf { it.amountMinor } }
        bucketGauges.forEach { (bucket, gauge) ->
            gauge.set(totals[bucket] ?: 0)
        }
    }
}
