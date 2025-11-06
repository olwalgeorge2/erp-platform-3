package com.erp.identity.domain.model.tenant

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class SubscriptionTest {
    @Test
    fun `constructor enforces positive limits`() {
        assertThrows(IllegalArgumentException::class.java) {
            Subscription(
                plan = SubscriptionPlan.STARTER,
                startDate = Instant.now(),
                endDate = null,
                maxUsers = 0,
                maxStorage = 10,
                features = emptySet(),
            )
        }
    }

    @Test
    fun `isExpired returns true when end date is in the past`() {
        val subscription =
            Subscription(
                plan = SubscriptionPlan.STARTER,
                startDate = Instant.now().minusSeconds(3600),
                endDate = Instant.now().minusSeconds(60),
                maxUsers = 10,
                maxStorage = 10,
                features = emptySet(),
            )

        assertTrue(subscription.isExpired())
        assertFalse(subscription.isActive())
    }

    @Test
    fun `isActive respects start date`() {
        val subscription =
            Subscription(
                plan = SubscriptionPlan.STARTER,
                startDate = Instant.now().plusSeconds(3600),
                endDate = null,
                maxUsers = 10,
                maxStorage = 10,
                features = setOf("billing"),
            )

        assertFalse(subscription.isActive())
        assertTrue(subscription.hasFeature("billing"))
    }
}
