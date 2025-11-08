plugins {
    id("erp.kotlin-conventions")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.2")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.2.1")
}

tasks.named<Test>("test") {
    // Opt-in execution: run with -PrunArchTests=true
    val runArch = providers.gradleProperty("runArchTests").map { it.equals("true", ignoreCase = true) }.orElse(false)
    enabled = runArch.get()
    useJUnitPlatform()
}

// Helper task: generate/update frozen baseline without failing the build
tasks.register<Test>("archFreezeBaseline") {
    description = "Generate/update ArchUnit frozen baseline (ADR-006)"
    group = "verification"
    // Always enabled for explicit baseline generation
    enabled = true
    useJUnitPlatform()
    // Run the same test set as default 'test'
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    // Ensure freeze writes baseline
    systemProperty("archunit.freeze.store.default.allowStoreCreation", "true")
    systemProperty("archunit.freeze.refreeze", "true")
    // Do not fail the task on existing violations while capturing baseline
    ignoreFailures = true
}
