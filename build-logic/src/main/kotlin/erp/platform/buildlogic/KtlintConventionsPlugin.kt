package erp.platform.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin that applies ktlint code style checks.
 * This ensures consistent Kotlin code formatting across all modules.
 */
class KtlintConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        }
    }
}
