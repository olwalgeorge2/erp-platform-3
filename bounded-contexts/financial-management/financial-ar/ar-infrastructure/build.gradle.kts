import org.gradle.api.tasks.testing.Test

plugins {
    id("erp.quarkus-conventions")
    alias(libs.plugins.kotlin.noarg)
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

dependencies {
    implementation(project(":bounded-contexts:financial-management:financial-ar:ar-application"))
    implementation(project(":bounded-contexts:financial-management:financial-ar:ar-domain"))
    implementation(project(":bounded-contexts:financial-management:financial-shared"))
    implementation(project(":bounded-contexts:financial-management:financial-accounting:accounting-domain"))
    implementation(project(":bounded-contexts:financial-management:financial-accounting:accounting-application"))

    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation(libs.jackson.module.kotlin)

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
}

tasks.named<Test>("test") {
    enabled = true
}
