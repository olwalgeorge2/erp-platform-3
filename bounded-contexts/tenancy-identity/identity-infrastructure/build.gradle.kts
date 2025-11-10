// Identity Infrastructure module
// - Adapters: REST (JAX-RS), messaging (Kafka), persistence (JPA/Flyway)
// - Integration tests use Testcontainers (Postgres/Kafka). Enable with: -DwithContainers=true
plugins {
    id("erp.quarkus-conventions")
}

// Ensure IDEs honor the same JVM bytecode target as the toolchain
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        // Future-proof Jackson annotations on constructor params
        // See KT-73255: apply annotation to both param and property to avoid warnings
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    // Internal modules
    implementation(project(":bounded-contexts:tenancy-identity:identity-application"))
    implementation(project(":bounded-contexts:tenancy-identity:identity-domain"))
    implementation(project(":platform-shared:common-types"))

    // Persistence: Postgres + Flyway migrations
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")

    // JSON: Kotlin module (migrate to version catalog when available)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.1")

    // Scheduling + Bean validation
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-hibernate-validator")

    // Observability + Messaging + Config
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-messaging-kafka")
    implementation("io.quarkus:quarkus-config-yaml")

    // Security: Credential hashing (Argon2id)
    implementation("de.mkammerer:argon2-jvm:2.11")

    // Kotlin reflection (required by some frameworks/serializers)
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Testing: Quarkus + REST Assured + Mockito + Testcontainers
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5-mockito")
    // Use version catalog for REST Assured for consistency across modules
    testImplementation(libs.rest.assured)
    // Mockito-Kotlin (consider moving to version catalog later)
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    // Testcontainers: core/JUnit + Postgres/Kafka modules
    testImplementation("org.testcontainers:junit-jupiter:1.20.1")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation("org.testcontainers:kafka:1.20.1")
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
    // Run with: ./gradlew test -DwithContainers=true
    // Naming conventions matched by excludes: *IntegrationTest*, *IT*
    val withContainers = System.getProperty("withContainers", "false").toBoolean()
    if (!withContainers) {
        exclude("**/*IntegrationTest*")
        exclude("**/*IT*")
    }
}

// Enable tests (override convention plugin's disabled state)
tasks.named<Test>("test") {
    enabled = true
}
