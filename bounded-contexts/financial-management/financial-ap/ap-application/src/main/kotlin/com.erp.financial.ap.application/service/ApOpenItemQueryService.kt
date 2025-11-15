package com.erp.financial.ap.application.service

import com.erp.financial.ap.application.port.input.query.AgingBucket
import com.erp.financial.ap.application.port.input.query.AgingDetailLine
import com.erp.financial.ap.application.port.input.query.AgingDetailResult
import com.erp.financial.ap.application.port.input.query.AgingSummaryLine
import com.erp.financial.ap.application.port.input.query.AgingSummaryResult
import com.erp.financial.ap.application.port.input.query.ApAgingQuery
import com.erp.financial.ap.application.port.input.query.ApOpenItemQueryUseCase
import com.erp.financial.ap.application.port.output.ApOpenItemFilter
import com.erp.financial.ap.application.port.output.ApOpenItemRepository
import com.erp.financial.ap.domain.model.openitem.ApOpenItemStatus
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicLong

@ApplicationScoped
class ApOpenItemQueryService(
    private val repository: ApOpenItemRepository,
    meterRegistry: MeterRegistry,
) : ApOpenItemQueryUseCase {
    private val bucketGauges: Map<AgingBucket, AtomicLong> =
        AgingBucket
            .values()
            .associateWith { bucket ->
                meterRegistry.gauge(
                    "ap.open_items.amount_minor",
                    listOf(
                        io.micrometer.core.instrument.Tag
                            .of("bucket", bucket.name.lowercase()),
                    ),
                    AtomicLong(0),
                )!!
            }

    override fun getAgingDetail(query: ApAgingQuery): AgingDetailResult {
        val lines = computeLines(query)
        recordMetrics(lines)
        return AgingDetailResult(query.asOfDate, lines)
    }

    override fun getAgingSummary(query: ApAgingQuery): AgingSummaryResult {
        val lines = computeLines(query)
        recordMetrics(lines)
        val buckets =
            lines
                .groupBy { it.bucket }
                .map { (bucket, bucketLines) ->
                    AgingSummaryLine(
                        bucket = bucket,
                        totalAmountMinor = bucketLines.sumOf { it.amountMinor },
                        count = bucketLines.size.toLong(),
                    )
                }.sortedBy { it.bucket.ordinal }
        return AgingSummaryResult(query.asOfDate, buckets)
    }

    private fun computeLines(query: ApAgingQuery): List<AgingDetailLine> {
        val items =
            repository.list(
                ApOpenItemFilter(
                    tenantId = query.tenantId,
                    companyCodeId = query.companyCodeId,
                    vendorId = query.vendorId,
                    statuses = setOf(ApOpenItemStatus.OPEN, ApOpenItemStatus.PARTIALLY_PAID),
                ),
            )
        return items.map { item ->
            val bucket = determineBucket(item.dueDate, query.asOfDate)
            AgingDetailLine(
                openItemId = item.id,
                invoiceId = item.invoiceId,
                vendorId = item.vendorId,
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
        asOfDate: LocalDate,
    ): AgingBucket {
        val daysPastDue = ChronoUnit.DAYS.between(dueDate, asOfDate)
        return when {
            daysPastDue <= 0 -> AgingBucket.CURRENT
            daysPastDue <= 30 -> AgingBucket.DAYS_1_30
            daysPastDue <= 60 -> AgingBucket.DAYS_31_60
            daysPastDue <= 90 -> AgingBucket.DAYS_61_90
            else -> AgingBucket.DAYS_91_PLUS
        }
    }

    private fun recordMetrics(lines: List<AgingDetailLine>) {
        val totals =
            lines
                .groupBy { it.bucket }
                .mapValues { (_, bucketLines) -> bucketLines.sumOf { it.amountMinor } }
        bucketGauges.forEach { (bucket, gauge) ->
            gauge.set(totals[bucket] ?: 0)
        }
    }
}
