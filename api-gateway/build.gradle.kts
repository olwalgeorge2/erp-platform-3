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

    // Logging API
    implementation(libs.slf4j.api)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.rest.assured)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.wiremock)
}
