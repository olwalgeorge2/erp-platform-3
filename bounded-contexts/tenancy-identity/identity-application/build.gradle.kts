plugins {
    id("erp.quarkus-conventions")
}

dependencies {
    implementation(project(":bounded-contexts:tenancy-identity:identity-domain"))
    implementation(project(":platform-shared:common-types"))
}
