plugins {
    id("erp.kotlin-conventions")
}

dependencies {
    implementation(project(":bounded-contexts:financial-management:financial-shared"))
    implementation(project(":bounded-contexts:financial-management:financial-accounting:accounting-domain"))
}
