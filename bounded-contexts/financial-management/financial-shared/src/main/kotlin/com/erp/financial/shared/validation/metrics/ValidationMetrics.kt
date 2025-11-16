package com.erp.financial.shared.validation.metrics

import io.micrometer.core.instrument.MeterRegistry
import java.util.concurrent.TimeUnit

object ValidationMetrics {
    const val REQUEST_START_ATTRIBUTE = "validationStartTimeNano"

    @Volatile
    private var meterRegistry: MeterRegistry? = null

    @Volatile
    private var boundedContext: String = "finance"

    fun initialize(meterRegistry: MeterRegistry, boundedContext: String) {
        this.meterRegistry = meterRegistry
        this.boundedContext = boundedContext
    }

    fun recordRequest(
        method: String,
        pathTemplate: String,
        status: Int,
        durationNanos: Long,
        validationFailure: Boolean,
    ) {
        val registry = meterRegistry ?: return
        registry
            .timer(
                "validation.request.duration",
                "bounded_context",
                boundedContext,
                "method",
                method,
                "path",
                pathTemplate,
            ).record(durationNanos, TimeUnit.NANOSECONDS)

        registry
            .counter(
                "validation.request.total",
                "bounded_context",
                boundedContext,
                "method",
                method,
                "path",
                pathTemplate,
                "outcome",
                if (validationFailure) "failure" else "success",
                "status",
                status.toString(),
            ).increment()
    }

    fun recordRule(rule: String, durationNanos: Long, success: Boolean) {
        val registry = meterRegistry ?: return
        val result = if (success) "pass" else "fail"
        registry
            .timer(
                "validation.rule.duration",
                "bounded_context",
                boundedContext,
                "rule",
                rule,
                "result",
                result,
            ).record(durationNanos, TimeUnit.NANOSECONDS)
        registry
            .counter(
                "validation.rule.total",
                "bounded_context",
                boundedContext,
                "rule",
                rule,
                "result",
                result,
            ).increment()
    }

    fun recordRateLimitViolation(
        bucket: String,
        keyType: String,
    ) {
        val registry = meterRegistry ?: return
        registry
            .counter(
                "validation.rate_limit.violations",
                "bounded_context",
                boundedContext,
                "bucket",
                bucket,
                "key_type",
                keyType,
            ).increment()
    }

    fun durationMillis(requestStart: Long?): Long? =
        requestStart?.let { (System.nanoTime() - it) / 1_000_000 }
}
