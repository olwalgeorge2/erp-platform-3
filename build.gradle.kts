plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.allopen) apply false
    alias(libs.plugins.quarkus) apply false
    alias(libs.plugins.kover) apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
    id("org.owasp.dependencycheck") version "10.0.4"
}

// Configure OWASP Dependency Check
dependencyCheck {
    outputDirectory = "${project.layout.buildDirectory.get()}/reports/dependency-check"
    format = "ALL"
    failBuildOnCVSS = 7.0f
    suppressionFile = file("$rootDir/dependency-check-suppressions.xml").takeIf { it.exists() }?.absolutePath
    
    // CI-friendly configuration
    autoUpdate = false  // Don't auto-update in CI to avoid API issues
    
    analyzers {
        // Keep essential analyzers, disable problematic ones
        assemblyEnabled = false
        nuspecEnabled = false 
        nugetconfEnabled = false
    }
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

// Convenience preflight task for local verification
tasks.register("verifyLocal") {
    group = "verification"
    description = "Run ktlint + architecture tests + identity infra tests"
    dependsOn(
        ":tests:arch:test",
        ":bounded-contexts:tenancy-identity:identity-infrastructure:test",
    )
}

gradle.projectsEvaluated {
    val verify = tasks.named("verifyLocal")
    subprojects.forEach { p ->
        if (p.tasks.findByName("ktlintCheck") != null) {
            verify.configure { dependsOn(p.path + ":ktlintCheck") }
        }
    }
}

// Make filtered test runs not fail tasks when no tests match in a given subproject
subprojects {
    tasks.withType<Test>().configureEach {
        // keep JUnit platform default from conventions; only adjust filter behavior
        this.filter.isFailOnNoMatchingTests = false
    }
}
// --- Smoke test helper -------------------------------------------------------
tasks.register<Exec>("smokeIdentity") {
    group = "verification"
    description = "Runs tenancy-identity + Kafka smoke checks"

    val osName = System.getProperty("os.name").lowercase(java.util.Locale.getDefault())
    if (osName.contains("windows")) {
        commandLine("pwsh", "-NoLogo", "-File", "scripts/smoke/smoke-identity.ps1")
    } else {
        commandLine("bash", "scripts/smoke/smoke-identity.sh")
    }
    // inherit environment (IDENTITY_BASE_URL if set)
    environment(System.getenv())
}
