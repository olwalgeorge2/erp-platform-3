plugins {
    id("erp.quarkus-conventions")
}

dependencies {
    implementation(project(":bounded-contexts:financial-management:financial-accounting:accounting-domain"))
    implementation(project(":bounded-contexts:financial-management:financial-shared"))
    implementation(project(":platform-shared:common-types"))

    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
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
    }
}

tasks.named<Test>("test") {
    enabled = true
}
