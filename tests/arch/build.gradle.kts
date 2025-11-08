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
