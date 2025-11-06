plugins {
    id("erp.kotlin-conventions")
}

dependencies {
    implementation(project(":platform-shared:common-types"))
}

tasks.withType<Test>().configureEach {
    enabled = true
}
