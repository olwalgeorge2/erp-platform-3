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

    // JSON: Kotlin module
    implementation(libs.jackson.module.kotlin)

    // Scheduling + Bean validation
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-hibernate-validator")

    // Observability + Messaging + Config
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-messaging-kafka")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation(libs.quarkus.smallrye.openapi)

    // Security: Credential hashing (Argon2id)
    implementation("de.mkammerer:argon2-jvm:2.11")

    // Kotlin reflection (required by some frameworks/serializers)
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Testing: Quarkus + REST Assured + Mockito + Testcontainers
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5-mockito")
    // Use version catalog for REST Assured for consistency across modules
    testImplementation(libs.rest.assured)
    testImplementation(libs.mockito.kotlin)
    // Testcontainers: core/JUnit + Postgres/Kafka modules (catalog aligns all to 1.x)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
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
    // Naming conventions matched by excludes: *IntegrationTest*, *IT*
    val withContainers = (findProperty("withContainers") as String?)?.toBoolean() ?: false
    if (!withContainers) {
        exclude("**/*IntegrationTest*.class")
        exclude("**/*IT*.class")
    }
}

// Enable tests (override convention plugin's disabled state)
tasks.named<Test>("test") {
    enabled = true
}

// Quarkus code generation expects the Java classes output directory to exist even when the module is Kotlin-only.
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
