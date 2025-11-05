plugins {
    `kotlin-dsl`
}

val libs = extensions.getByType(org.gradle.api.artifacts.VersionCatalogsExtension::class.java).named("buildLogicLibs")

kotlin {
    jvmToolchain(libs.findVersion("java").get().requiredVersion.toInt())
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.findVersion("kotlin").get().requiredVersion}")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.1.1")
}

gradlePlugin {
    plugins {
        create("kotlinConventions") {
            id = "erp.kotlin-conventions"
            implementationClass = "erp.platform.buildlogic.KotlinConventionsPlugin"
            displayName = "ERP Kotlin JVM conventions"
            description = "Central Kotlin JVM conventions for the ERP platform"
        }
        create("quarkusConventions") {
            id = "erp.quarkus-conventions"
            implementationClass = "erp.platform.buildlogic.QuarkusConventionsPlugin"
            displayName = "ERP Quarkus service conventions"
            description = "Quarkus application conventions shared by service modules"
        }
        create("nativeImageConventions") {
            id = "erp.native-image-conventions"
            implementationClass = "erp.platform.buildlogic.NativeImageConventionsPlugin"
            displayName = "ERP Quarkus native image conventions"
            description = "Native image configuration shared across Quarkus services"
        }
        create("ktlintConventions") {
            id = "erp.ktlint-conventions"
            implementationClass = "erp.platform.buildlogic.KtlintConventionsPlugin"
            displayName = "ERP ktlint conventions"
            description = "Code style conventions using ktlint"
        }
    }
}
