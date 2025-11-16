plugins {
    id("erp.kotlin-conventions")
}

dependencies {
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
    implementation("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")
    implementation("org.eclipse.microprofile.openapi:microprofile-openapi-api:3.1")
    implementation("org.jboss.logging:jboss-logging:3.5.0.Final")
    implementation("io.quarkus:quarkus-jackson:${libs.versions.quarkus.get()}")
    implementation(libs.jackson.module.kotlin)

    // Bean Validation for custom validators
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    implementation("io.quarkus:quarkus-hibernate-validator:${libs.versions.quarkus.get()}")
    implementation("org.jetbrains.kotlin:kotlin-reflect") // For reflection-based field access in validators

    // Metrics for validation monitoring
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus:${libs.versions.quarkus.get()}")
    implementation("io.quarkus:quarkus-smallrye-fault-tolerance:${libs.versions.quarkus.get()}")

    // Input sanitization (XSS/injection prevention)
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20220608.1")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testImplementation("org.hibernate.validator:hibernate-validator:8.0.1.Final")
    testImplementation("org.glassfish.expressly:expressly:5.0.0") // EL implementation for Hibernate Validator
    testImplementation("org.mockito:mockito-core:5.5.0") // For mocking ConstraintValidatorContext
}

// Enable tests (override convention plugin's disabled state)
tasks.withType<Test>().configureEach {
    enabled = true
    useJUnitPlatform()

    // Show test results in console
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true

        // Show individual test names and results
        showStandardStreams = false
    }
}

// Explicitly enable the test task
tasks.named<Test>("test") {
    enabled = true
}
