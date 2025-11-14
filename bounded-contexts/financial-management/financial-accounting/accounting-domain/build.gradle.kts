plugins {
    id("erp.kotlin-conventions")
}

dependencies {
    implementation(project(":platform-shared:common-types"))
}

tasks.withType<Test>().configureEach {
    enabled = true
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
