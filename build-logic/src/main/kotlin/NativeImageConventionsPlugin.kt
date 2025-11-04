package erp.platform.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.api.artifacts.VersionCatalogsExtension

class NativeImageConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(QuarkusConventionsPlugin::class.java)

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        dependencies {
            add("implementation", libs.findLibrary("quarkus-elytron-security").orElseThrow())
            add("implementation", libs.findLibrary("quarkus-opentelemetry").orElseThrow())
            add("implementation", libs.findLibrary("quarkus-cache").orElseThrow())
            add("implementation", libs.findLibrary("quarkus-jdbc-postgresql").orElseThrow())
        }

        val quarkusExtension = extensions.findByName("quarkus")
        if (quarkusExtension != null) {
            val nativeConfig = quarkusExtension.javaClass.methods
                .firstOrNull { it.name == "getNative" && it.parameterCount == 0 }
                ?.invoke(quarkusExtension)
                ?: return@with

            val nativeClass = nativeConfig::class.java

            nativeClass.methods.firstOrNull { it.name == "setContainerBuild" && it.parameterCount == 1 }
                ?.invoke(nativeConfig, java.lang.Boolean.TRUE)

            nativeClass.methods.firstOrNull { it.name == "setBuilderImage" && it.parameterCount == 1 }
                ?.invoke(nativeConfig, "quay.io/quarkus/ubi-quarkus-native-image:${libs.findVersion("quarkus").get().requiredVersion}")

            val additionalArgs = nativeClass.methods
                .firstOrNull { it.name == "getAdditionalBuildArgs" && it.parameterCount == 0 }
                ?.invoke(nativeConfig) as? MutableCollection<Any?>

            additionalArgs?.add("--verbose")
        }
    }
}
