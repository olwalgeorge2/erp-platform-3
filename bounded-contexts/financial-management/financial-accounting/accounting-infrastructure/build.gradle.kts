// Financial Accounting infrastructure (REST adapters, persistence, messaging)
plugins {
    id("erp.quarkus-conventions")
    alias(libs.plugins.kotlin.noarg)
}

// Configure noarg plugin for JPA entities
noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

dependencies {
    implementation(project(":bounded-contexts:financial-management:financial-accounting:accounting-application"))
    implementation(project(":bounded-contexts:financial-management:financial-accounting:accounting-domain"))
    implementation(project(":platform-shared:common-types"))

    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-messaging-kafka")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation(libs.jackson.module.kotlin)

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    // Testcontainers for integration tests
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.testcontainers:kafka:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.apache.kafka:kafka-clients:3.9.0")
}

tasks.withType<Test>().configureEach {
    enabled = true
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    val withContainers = (findProperty("withContainers") as String?)?.toBoolean() ?: false
    if (!withContainers) {
        exclude("**/*IntegrationTest*")
        exclude("**/*IT*")
    } else {
        // Pass system property to JUnit for @EnabledIfSystemProperty check
        systemProperty("withContainers", "true")
    }
}

tasks.named<Test>("test") {
    enabled = true
}

val ensureJavaClassesDir by tasks.registering {
    val classesDir = layout.buildDirectory.dir("classes/java/main")
    outputs.dir(classesDir)
    doLast {
        classesDir.get().asFile.mkdirs()
    }
}

tasks.named("quarkusGenerateCodeTests") {
    dependsOn(ensureJavaClassesDir)
}

tasks.named("classes") {
    dependsOn(ensureJavaClassesDir)
}
