plugins {
    id("erp.quarkus-conventions")
}

dependencies {
    implementation(libs.quarkus.rest)
    implementation(libs.quarkus.rest.jackson)
    implementation(libs.slf4j.api)

    testImplementation(libs.junit.jupiter)
}
