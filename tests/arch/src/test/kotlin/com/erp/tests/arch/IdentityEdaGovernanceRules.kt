package com.erp.tests.arch

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.freeze.FreezingArchRule
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Identity EDA governance rules aligned to ADR-007.
 *
 * - Application layer must not depend on Kafka/Reactive Messaging
 * - Outbox components must live in infrastructure
 */
class IdentityEdaGovernanceRules {
    companion object {
        private lateinit var identityClasses: JavaClasses

        @JvmStatic
        @BeforeAll
        fun importClasses() {
            identityClasses =
                ClassFileImporter()
                    .withImportOption(ImportOption.DoNotIncludeTests())
                    .importPackages("com.erp.identity..")
        }
    }

    @Test
    fun `application layer must not depend on Kafka or Reactive Messaging`() {
        val rule: ArchRule =
            noClasses()
                .that()
                .resideInAnyPackage(
                    "com.erp.identity..application..",
                ).should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                    "org.apache.kafka..",
                    "io.smallrye.reactive.messaging..",
                    "org.eclipse.microprofile.reactive.messaging..",
                ).because(
                    "application layer should depend on ports, not Kafka adapters (ADR-007)",
                )

        FreezingArchRule.freeze(rule).check(identityClasses)
    }

    @Test
    fun `outbox components must live in infrastructure`() {
        val rule: ArchRule =
            classes()
                .that()
                .haveSimpleNameEndingWith("OutboxEventScheduler")
                .or()
                .haveSimpleNameEndingWith("OutboxEventPublisher")
                .or()
                .haveSimpleNameEndingWith("OutboxMessagePublisher")
                .or()
                .haveSimpleNameEndingWith("OutboxRepository")
                .or()
                .haveSimpleNameEndingWith("OutboxEventEntity")
                .should()
                .resideInAnyPackage("com.erp.identity..infrastructure..")
                .because("outbox is an infrastructure concern (ADR-007)")

        FreezingArchRule.freeze(rule).check(identityClasses)
    }

    @Test
    fun `event consumers must reside in infrastructure`() {
        val rule: ArchRule =
            classes()
                .that()
                .haveSimpleNameEndingWith("EventConsumer")
                .should()
                .resideInAnyPackage("com.erp.identity..infrastructure..")
                .because("consumers are infrastructure adapters (ADR-007)")

        FreezingArchRule.freeze(rule).check(identityClasses)
    }

    @Test
    fun `domain events must be immutable data classes`() {
        val rule: ArchRule =
            classes()
                .that()
                .implement("com.erp.shared.types.events.DomainEvent")
                .and()
                .resideInAnyPackage("com.erp.identity..domain.events..")
                .should()
                .haveOnlyFinalFields()
                .because("domain events must be immutable (ADR-007)")

        FreezingArchRule.freeze(rule).check(identityClasses)
    }

    @Test
    fun `event publishers must not publish directly to Kafka`() {
        val rule: ArchRule =
            noClasses()
                .that()
                .resideInAnyPackage("com.erp.identity..application..")
                .should()
                .accessClassesThat()
                .resideInAnyPackage(
                    "org.apache.kafka.clients.producer..",
                ).because(
                    "application layer must use EventPublisherPort abstraction, not Kafka Producer (ADR-007)",
                )

        FreezingArchRule.freeze(rule).check(identityClasses)
    }
}
