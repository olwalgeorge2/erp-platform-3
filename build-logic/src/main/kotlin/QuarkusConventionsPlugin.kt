package erp.platform.buildlogic

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.kotlin.dsl.*

class QuarkusConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(KotlinConventionsPlugin::class.java)
        pluginManager.apply("io.quarkus")
        pluginManager.apply("org.jetbrains.kotlin.plugin.allopen")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val quarkusVersion = libs.findVersion("quarkus").get().requiredVersion
        val quarkusCoreLibs = listOf(
            "quarkus-arc",
            "quarkus-rest",
            "quarkus-rest-jackson",
            "quarkus-hibernate-orm",
            "quarkus-hibernate-validator",
            "quarkus-logging-json",
            "quarkus-messaging",
            "quarkus-messaging-kafka",
            "quarkus-kafka-client"
        )

        dependencies {
            add("implementation", dependencies.platform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
            quarkusCoreLibs.forEach { alias ->
                add("implementation", libs.findLibrary(alias).orElseThrow())
            }
            add("implementation", libs.findLibrary("slf4j-api").orElseThrow())
            add("runtimeOnly", libs.findLibrary("slf4j-simple").orElseThrow())
            add("testImplementation", libs.findLibrary("junit-jupiter").orElseThrow())
        }

        tasks.withType(Test::class.java).configureEach {
            useJUnitPlatform()
            systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
            
            // Skip tests by default (enable per-module when ready)
            enabled = false
        }

        val quarkusExtension = extensions.findByName("quarkus")
        if (quarkusExtension != null) {
            quarkusExtension.javaClass.methods
                .firstOrNull { it.name == "setWarnIfBuildProfileMissing" && it.parameterCount == 1 }
                ?.invoke(quarkusExtension, java.lang.Boolean.TRUE)
        }
    }
}

