plugins {
    id("erp.kotlin-conventions")
}

dependencies {
    implementation(project(":platform-shared:common-types"))
}

tasks.withType<Test>().configureEach {
    enabled = true

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
