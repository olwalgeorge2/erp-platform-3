plugins {
    id("erp.kotlin-conventions")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.2")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.2.1")
    // Provide classes under test at runtime so ArchUnit can see real packages
    testRuntimeOnly(project(":platform-shared:common-types"))
    testRuntimeOnly(project(":platform-shared:common-security"))
    testRuntimeOnly(project(":platform-shared:common-observability"))
    testRuntimeOnly(project(":platform-shared:common-messaging"))
    // At least one bounded context to validate coupling rules
    testRuntimeOnly(project(":bounded-contexts:tenancy-identity:identity-domain"))
    testRuntimeOnly(project(":bounded-contexts:tenancy-identity:identity-application"))
    testRuntimeOnly(project(":bounded-contexts:tenancy-identity:identity-infrastructure"))
    // Financial management context (Sprint 3 expansion)
    testRuntimeOnly(project(":bounded-contexts:financial-management:financial-shared"))
    testRuntimeOnly(project(":bounded-contexts:financial-management:financial-accounting:accounting-domain"))
    testRuntimeOnly(project(":bounded-contexts:financial-management:financial-accounting:accounting-application"))
    testRuntimeOnly(project(":bounded-contexts:financial-management:financial-accounting:accounting-infrastructure"))
    testRuntimeOnly(project(":bounded-contexts:financial-management:financial-ar:ar-domain"))
    testRuntimeOnly(project(":bounded-contexts:financial-management:financial-ar:ar-application"))
    testRuntimeOnly(project(":bounded-contexts:financial-management:financial-ar:ar-infrastructure"))
    testRuntimeOnly(project(":bounded-contexts:financial-management:financial-ap:ap-domain"))
    testRuntimeOnly(project(":bounded-contexts:financial-management:financial-ap:ap-application"))
    testRuntimeOnly(project(":bounded-contexts:financial-management:financial-ap:ap-infrastructure"))

    // Commerce context (Sprint 3 expansion)
    // B2B
    testRuntimeOnly(project(":bounded-contexts:commerce:commerce-b2b:b2b-domain"))
    testRuntimeOnly(project(":bounded-contexts:commerce:commerce-b2b:b2b-application"))
    testRuntimeOnly(project(":bounded-contexts:commerce:commerce-b2b:b2b-infrastructure"))
    // eCommerce
    testRuntimeOnly(project(":bounded-contexts:commerce:commerce-ecommerce:ecommerce-domain"))
    testRuntimeOnly(project(":bounded-contexts:commerce:commerce-ecommerce:ecommerce-application"))
    testRuntimeOnly(project(":bounded-contexts:commerce:commerce-ecommerce:ecommerce-infrastructure"))
    // Marketplace
    testRuntimeOnly(project(":bounded-contexts:commerce:commerce-marketplace:marketplace-domain"))
    testRuntimeOnly(project(":bounded-contexts:commerce:commerce-marketplace:marketplace-application"))
    testRuntimeOnly(project(":bounded-contexts:commerce:commerce-marketplace:marketplace-infrastructure"))
    // POS
    testRuntimeOnly(project(":bounded-contexts:commerce:commerce-pos:pos-domain"))
    testRuntimeOnly(project(":bounded-contexts:commerce:commerce-pos:pos-application"))
    testRuntimeOnly(project(":bounded-contexts:commerce:commerce-pos:pos-infrastructure"))

    // Business Intelligence
    testRuntimeOnly(project(":bounded-contexts:business-intelligence:bi-domain"))
    testRuntimeOnly(project(":bounded-contexts:business-intelligence:bi-application"))
    testRuntimeOnly(project(":bounded-contexts:business-intelligence:bi-infrastructure"))

    // Communication Hub
    testRuntimeOnly(project(":bounded-contexts:communication-hub:communication-domain"))
    testRuntimeOnly(project(":bounded-contexts:communication-hub:communication-application"))
    testRuntimeOnly(project(":bounded-contexts:communication-hub:communication-infrastructure"))

    // Corporate Services
    testRuntimeOnly(project(":bounded-contexts:corporate-services:corporate-assets:assets-domain"))
    testRuntimeOnly(project(":bounded-contexts:corporate-services:corporate-assets:assets-application"))
    testRuntimeOnly(project(":bounded-contexts:corporate-services:corporate-assets:assets-infrastructure"))
    testRuntimeOnly(project(":bounded-contexts:corporate-services:corporate-hr:hr-domain"))
    testRuntimeOnly(project(":bounded-contexts:corporate-services:corporate-hr:hr-application"))
    testRuntimeOnly(project(":bounded-contexts:corporate-services:corporate-hr:hr-infrastructure"))

    // Customer Relation
    testRuntimeOnly(project(":bounded-contexts:customer-relation:customer-campaigns:campaigns-domain"))
    testRuntimeOnly(project(":bounded-contexts:customer-relation:customer-campaigns:campaigns-application"))
    testRuntimeOnly(project(":bounded-contexts:customer-relation:customer-campaigns:campaigns-infrastructure"))
    testRuntimeOnly(project(":bounded-contexts:customer-relation:customer-crm:crm-domain"))
    testRuntimeOnly(project(":bounded-contexts:customer-relation:customer-crm:crm-application"))
    testRuntimeOnly(project(":bounded-contexts:customer-relation:customer-crm:crm-infrastructure"))
    testRuntimeOnly(project(":bounded-contexts:customer-relation:customer-support:support-domain"))
    testRuntimeOnly(project(":bounded-contexts:customer-relation:customer-support:support-application"))
    testRuntimeOnly(project(":bounded-contexts:customer-relation:customer-support:support-infrastructure"))

    // Inventory Management
    testRuntimeOnly(project(":bounded-contexts:inventory-management:inventory-stock:stock-domain"))
    testRuntimeOnly(project(":bounded-contexts:inventory-management:inventory-stock:stock-application"))
    testRuntimeOnly(project(":bounded-contexts:inventory-management:inventory-stock:stock-infrastructure"))
    testRuntimeOnly(project(":bounded-contexts:inventory-management:inventory-warehouse:warehouse-domain"))
    testRuntimeOnly(project(":bounded-contexts:inventory-management:inventory-warehouse:warehouse-application"))
    testRuntimeOnly(project(":bounded-contexts:inventory-management:inventory-warehouse:warehouse-infrastructure"))

    // Manufacturing Execution
    testRuntimeOnly(project(":bounded-contexts:manufacturing-execution:manufacturing-maintenance:maintenance-domain"))
    testRuntimeOnly(project(":bounded-contexts:manufacturing-execution:manufacturing-maintenance:maintenance-application"))
    testRuntimeOnly(project(":bounded-contexts:manufacturing-execution:manufacturing-maintenance:maintenance-infrastructure"))
    testRuntimeOnly(project(":bounded-contexts:manufacturing-execution:manufacturing-production:production-domain"))
    testRuntimeOnly(project(":bounded-contexts:manufacturing-execution:manufacturing-production:production-application"))
    testRuntimeOnly(project(":bounded-contexts:manufacturing-execution:manufacturing-production:production-infrastructure"))
    testRuntimeOnly(project(":bounded-contexts:manufacturing-execution:manufacturing-quality:quality-domain"))
    testRuntimeOnly(project(":bounded-contexts:manufacturing-execution:manufacturing-quality:quality-application"))
    testRuntimeOnly(project(":bounded-contexts:manufacturing-execution:manufacturing-quality:quality-infrastructure"))

    // Operations Service
    testRuntimeOnly(project(":bounded-contexts:operations-service:operations-field-service:field-service-domain"))
    testRuntimeOnly(project(":bounded-contexts:operations-service:operations-field-service:field-service-application"))
    testRuntimeOnly(project(":bounded-contexts:operations-service:operations-field-service:field-service-infrastructure"))

    // Procurement
    testRuntimeOnly(project(":bounded-contexts:procurement:procurement-purchasing:purchasing-domain"))
    testRuntimeOnly(project(":bounded-contexts:procurement:procurement-purchasing:purchasing-application"))
    testRuntimeOnly(project(":bounded-contexts:procurement:procurement-purchasing:purchasing-infrastructure"))
    testRuntimeOnly(project(":bounded-contexts:procurement:procurement-sourcing:sourcing-domain"))
    testRuntimeOnly(project(":bounded-contexts:procurement:procurement-sourcing:sourcing-application"))
    testRuntimeOnly(project(":bounded-contexts:procurement:procurement-sourcing:sourcing-infrastructure"))
}

tasks.named<Test>("test") {
    // Always-on in enforcement mode
    enabled = true
    useJUnitPlatform()
}

// Helper task: generate/update frozen baseline without failing the build
tasks.register<Test>("archFreezeBaseline") {
    description = "Generate/update ArchUnit frozen baseline (ADR-006)"
    group = "verification"
    // Always enabled for explicit baseline generation
    enabled = true
    useJUnitPlatform()
    // Run the same test set as default 'test'
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    // Ensure freeze writes baseline
    systemProperty("archunit.freeze.store.default.allowStoreCreation", "true")
    systemProperty("archunit.freeze.refreeze", "true")
    // Do not fail the task on existing violations while capturing baseline
    ignoreFailures = true
}
