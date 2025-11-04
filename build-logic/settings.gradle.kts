rootProject.name = "build-logic"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("buildLogicLibs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
