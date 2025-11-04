rootProject.name = "erp-platform"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

val ignoredBuildDirs = setOf("build-logic", "buildSrc", ".gradle")

rootDir
    .walkTopDown()
    .filter { it.isFile && (it.name == "build.gradle.kts" || it.name == "build.gradle") }
    .map { it.parentFile }
    .filter { it != rootDir }
    .map { it.relativeTo(rootDir).invariantSeparatorsPath }
    .filterNot { segment -> ignoredBuildDirs.any { segment.startsWith(it) } }
    .map { path -> path.replace('/', ':') }
    .forEach { modulePath ->
        include(modulePath)
    }
