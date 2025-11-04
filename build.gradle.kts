plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.allopen) apply false
    alias(libs.plugins.quarkus) apply false
    alias(libs.plugins.kover) apply false
}

allprojects {
    group = "com.example.erp"
    version = "0.1.0-SNAPSHOT"
}
