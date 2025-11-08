package com.erp.tests.arch

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.freeze.FreezingArchRule
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Architecture tests enforcing ADR-006: Platform-Shared Governance Rules
 * 
 * These tests prevent the "distributed monolith" anti-pattern by ensuring:
 * - platform-shared contains only technical primitives
 * - No business logic or domain models leak into shared modules
 * - Bounded contexts remain autonomous
 * 
 * @see <a href="../../docs/adr/ADR-006-platform-shared-governance.md">ADR-006</a>
 */
class PlatformSharedGovernanceRules {

    companion object {
        private lateinit var allClasses: JavaClasses

        @JvmStatic
        @BeforeAll
        fun importClasses() {
            allClasses = ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .importPackages("com.erp..")
        }
    }

    @Test
    fun `platform-shared must not depend on bounded contexts`() {
        val rule: ArchRule =
            noClasses()
                .that().resideInAPackage("com.erp.shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                // Identity context
                "com.erp.identity..",
                // Financial contexts
                "com.erp.finance..",
                "com.erp.financial..",
                // Commerce contexts
                "com.erp.commerce..",
                // Inventory contexts
                "com.erp.inventory..",
                // Manufacturing contexts
                "com.erp.manufacturing..",
                // Procurement contexts
                "com.erp.procurement..",
                // Customer contexts
                "com.erp.customer..",
                // Corporate contexts
                "com.erp.corporate..",
                // Communication contexts
                "com.erp.communication..",
                // Operations contexts
                "com.erp.operations..",
                // BI contexts
                "com.erp.bi.."
                )
                .because("platform-shared must contain only technical primitives, " +
                        "not business domain concepts (ADR-006)")

        FreezingArchRule.freeze(rule).check(allClasses)
    }

    @Test
    fun `platform-shared must not depend on platform-infrastructure`() {
        val rule: ArchRule =
            noClasses()
                .that().resideInAPackage("com.erp.shared..")
                .should().dependOnClassesThat().resideInAPackage("com.erp.infrastructure..")
                .because("platform-shared should be the lowest-level module, " +
                        "containing only pure abstractions (ADR-006)")

        FreezingArchRule.freeze(rule).check(allClasses)
    }

    @Test
    fun `bounded contexts must not depend on each other directly`() {
        // Identity must not depend on other contexts
        val identityRule: ArchRule =
            noClasses()
                .that().resideInAPackage("com.erp.identity..")
                .should().dependOnClassesThat().resideInAnyPackage(
                "com.erp.finance..",
                "com.erp.commerce..",
                "com.erp.inventory..",
                "com.erp.manufacturing..",
                "com.erp.procurement..",
                "com.erp.customer..",
                "com.erp.corporate..",
                "com.erp.communication..",
                "com.erp.operations..",
                "com.erp.bi.."
                )
                .because("bounded contexts should communicate via events, " +
                        "not direct dependencies (ADR-003, ADR-006)")
        FreezingArchRule.freeze(identityRule).check(allClasses)

        // Finance must not depend on other contexts
        val financeRule: ArchRule =
            noClasses()
                .that().resideInAnyPackage("com.erp.finance..", "com.erp.financial..")
                .should().dependOnClassesThat().resideInAnyPackage(
                "com.erp.identity..",
                "com.erp.commerce..",
                "com.erp.inventory..",
                "com.erp.manufacturing..",
                "com.erp.procurement..",
                "com.erp.customer..",
                "com.erp.corporate..",
                "com.erp.communication..",
                "com.erp.operations..",
                "com.erp.bi.."
                )
                .because("bounded contexts should communicate via events, " +
                        "not direct dependencies (ADR-003, ADR-006)")
        FreezingArchRule.freeze(financeRule).check(allClasses)

        // Commerce must not depend on other contexts
        val commerceRule: ArchRule =
            noClasses()
                .that().resideInAPackage("com.erp.commerce..")
                .should().dependOnClassesThat().resideInAnyPackage(
                "com.erp.identity..",
                "com.erp.finance..",
                "com.erp.inventory..",
                "com.erp.manufacturing..",
                "com.erp.procurement..",
                "com.erp.customer..",
                "com.erp.corporate..",
                "com.erp.communication..",
                "com.erp.operations..",
                "com.erp.bi.."
                )
                .because("bounded contexts should communicate via events, " +
                        "not direct dependencies (ADR-003, ADR-006)")
        FreezingArchRule.freeze(commerceRule).check(allClasses)
    }

    @Test
    fun `platform-shared modules should only contain allowed types`() {
        // common-types should only contain technical abstractions (no Services/Repositories/"Business" types)
        noClasses()
            .that().resideInAPackage("com.erp.shared.types..")
            .should().haveSimpleNameEndingWith("Service")
            .because("platform-shared/common-types should not contain business services (ADR-006)")

        noClasses()
            .that().resideInAPackage("com.erp.shared.types..")
            .should().haveSimpleNameEndingWith("Repository")
            .because("platform-shared/common-types should not contain repositories (ADR-006)")

        noClasses()
            .that().resideInAPackage("com.erp.shared.types..")
            .should().haveSimpleNameContaining("Business")
            .because("platform-shared/common-types should not contain business logic (ADR-006)")
    }

    @Test
    fun `common-types should only contain abstractions not implementations`() {
        noClasses()
            .that().resideInAPackage("com.erp.shared.types..")
            .should().haveSimpleNameEndingWith("Impl")
            .because("platform-shared/common-types should contain interfaces and abstractions, " +
                    "not concrete implementations (ADR-006)")

        noClasses()
            .that().resideInAPackage("com.erp.shared.types..")
            .should().haveSimpleNameEndingWith("Adapter")
            .because("platform-shared/common-types should not contain adapters " +
                    "(those belong in infrastructure layer) (ADR-006)")
    }

    @Test
    fun `platform-shared should not contain JPA entities`() {
        val rule: ArchRule =
            noClasses()
                .that().resideInAPackage("com.erp.shared..")
                .should().beAnnotatedWith("jakarta.persistence.Entity")
                .orShould().beAnnotatedWith("jakarta.persistence.MappedSuperclass")
                .because("platform-shared should not contain persistence entities - " +
                        "those belong in bounded context infrastructure layers (ADR-006)")

        FreezingArchRule.freeze(rule).check(allClasses)
    }

    @Test
    fun `platform-shared should not contain REST resources`() {
        val rule: ArchRule =
            noClasses()
                .that().resideInAPackage("com.erp.shared..")
                .should().beAnnotatedWith("jakarta.ws.rs.Path")
                .because("platform-shared should not contain REST endpoints - " +
                        "those belong in bounded context application layers (ADR-006)")

        FreezingArchRule.freeze(rule).check(allClasses)
    }

    @Test
    fun `platform-shared should not contain Quarkus-specific services`() {
        val rule: ArchRule =
            noClasses()
                .that().resideInAPackage("com.erp.shared..")
                .should().beAnnotatedWith("jakarta.enterprise.context.ApplicationScoped")
                .orShould().beAnnotatedWith("jakarta.enterprise.context.RequestScoped")
                .because("platform-shared should contain pure abstractions, " +
                        "not framework-specific implementations (ADR-006)")

        FreezingArchRule.freeze(rule).check(allClasses)
    }
}
