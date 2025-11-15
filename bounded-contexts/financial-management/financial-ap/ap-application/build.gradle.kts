plugins {
    id("erp.quarkus-conventions")
    id("org.kordamp.gradle.jandex") version "2.1.0"
}

dependencies {
    implementation(project(":bounded-contexts:financial-management:financial-ap:ap-domain"))
    implementation(project(":bounded-contexts:financial-management:financial-shared"))
    implementation(project(":bounded-contexts:financial-management:financial-accounting:accounting-domain"))
    implementation("io.micrometer:micrometer-core")
}
