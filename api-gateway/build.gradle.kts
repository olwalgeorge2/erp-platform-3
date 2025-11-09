plugins {
    id("erp.quarkus-conventions")
}

configurations {
    // API Gateway does not use a JDBC datasource; avoid Agroal/ORM warnings
    implementation {
        exclude(group = "io.quarkus", module = "quarkus-hibernate-orm")
        exclude(group = "io.quarkus", module = "quarkus-agroal")
        exclude(group = "io.quarkus", module = "quarkus-jdbc-postgresql")
    }
}

dependencies {
    implementation(libs.quarkus.rest)
    implementation(libs.quarkus.rest.jackson)
    implementation(libs.slf4j.api)

    testImplementation(libs.junit.jupiter)
}
