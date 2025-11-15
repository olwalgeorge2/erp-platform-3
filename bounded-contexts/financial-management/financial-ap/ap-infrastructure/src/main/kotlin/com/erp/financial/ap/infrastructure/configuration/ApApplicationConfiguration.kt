package com.erp.financial.ap.infrastructure.configuration

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import java.time.Clock

@ApplicationScoped
class ApApplicationConfiguration {
    @Produces
    fun utcClock(): Clock = Clock.systemUTC()
}
