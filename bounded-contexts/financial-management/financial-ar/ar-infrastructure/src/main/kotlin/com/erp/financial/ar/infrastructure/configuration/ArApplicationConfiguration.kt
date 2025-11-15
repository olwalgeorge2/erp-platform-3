package com.erp.financial.ar.infrastructure.configuration

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import java.time.Clock

@ApplicationScoped
class ArApplicationConfiguration {
    @Produces
    fun utcClock(): Clock = Clock.systemUTC()
}
