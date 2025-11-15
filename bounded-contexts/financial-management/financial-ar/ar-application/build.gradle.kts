plugins {
    id("erp.quarkus-conventions")
}

dependencies {
    implementation(project(":bounded-contexts:financial-management:financial-ar:ar-domain"))
    implementation(project(":bounded-contexts:financial-management:financial-shared"))
    implementation(project(":bounded-contexts:financial-management:financial-accounting:accounting-domain"))
    implementation("io.micrometer:micrometer-core")
}
