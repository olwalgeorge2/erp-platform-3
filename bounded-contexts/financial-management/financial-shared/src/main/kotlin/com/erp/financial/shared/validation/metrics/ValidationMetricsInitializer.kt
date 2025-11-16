package com.erp.financial.shared.validation.metrics

import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.eclipse.microprofile.config.inject.ConfigProperty

@Singleton
class ValidationMetricsInitializer
    @Inject
    constructor(
        private val meterRegistry: MeterRegistry,
        @ConfigProperty(name = "validation.metrics.context", defaultValue = "finance")
        private val boundedContext: String,
    ) {
        init {
            ValidationMetrics.initialize(meterRegistry, boundedContext)
        }
    }
