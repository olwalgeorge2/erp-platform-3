plugins {
    id("erp.kotlin-conventions")
}

tasks.withType<Test>().configureEach {
    enabled = true
    useJUnitPlatform()
}
