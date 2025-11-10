plugins {
    id("erp.quarkus-conventions")
}

configurations {
    // API Gateway does not use a JDBC datasource; avoid Agroal/ORM warnings
    implementation {
        exclude(group = "io.quarkus", module = "quarkus-hibernate-orm")
        exclude(group = "io.quarkus", module = "quarkus-agroal")
        exclude(group = "io.quarkus", module = "quarkus-jdbc-postgresql")
    }
}

dependencies {
    // REST + JSON
    implementation(libs.quarkus.rest)
    implementation(libs.quarkus.rest.jackson)

    // REST client for proxying to services (non-reactive)
    implementation(libs.quarkus.rest.client)
    implementation(libs.quarkus.rest.client.jackson)

    // Security (JWT)
    implementation(libs.quarkus.smallrye.jwt)
    implementation(libs.quarkus.smallrye.jwt.build)

    // Rate limiting backend
    implementation(libs.quarkus.redis.client)

    // Observability
    implementation(libs.quarkus.micrometer.registry.prometheus)
    implementation(libs.quarkus.opentelemetry)
    implementation(libs.quarkus.logging.json)
    implementation(libs.quarkus.config.yaml)
    implementation(libs.quarkus.smallrye.health)

    // Logging API
    implementation(libs.slf4j.api)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.rest.assured)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.wiremock)
    testImplementation("io.quarkus:quarkus-junit5")
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

    // Skip integration tests requiring Testcontainers (Docker) unless explicitly enabled
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
