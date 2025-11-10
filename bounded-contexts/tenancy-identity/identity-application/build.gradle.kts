plugins {
    id("erp.quarkus-conventions")
}

dependencies {
    implementation(project(":bounded-contexts:tenancy-identity:identity-domain"))
    implementation(project(":platform-shared:common-types"))
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
}

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

    // Skip Testcontainers-based integration tests unless explicitly enabled
    // Run with: ./gradlew test -PwithContainers=true
    val withContainers = (findProperty("withContainers") as String?)?.toBoolean() ?: false
    if (!withContainers) {
        exclude("**/*IntegrationTest*")
        exclude("**/*IT*")
    }
}

// Enable tests (override convention plugin's disabled state)
tasks.named<Test>("test") {
    enabled = true
}
