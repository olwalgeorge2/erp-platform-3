plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.allopen) apply false
    alias(libs.plugins.quarkus) apply false
    alias(libs.plugins.kover) apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
}

allprojects {
    group = "com.example.erp"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    // Apply ktlint to all subprojects with Kotlin code
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")

        configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            version.set("1.3.1")
            verbose.set(true)
            outputToConsole.set(true)
            ignoreFailures.set(false)

            filter {
                exclude("**/generated/**")
                exclude("**/build/**")
            }
        }
    }
}
