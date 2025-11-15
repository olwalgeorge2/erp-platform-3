package com.erp.financial.ar.infrastructure.adapter.input.rest

import io.quarkus.test.junit.QuarkusTestProfile

class ArCommandValidationTestProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): MutableMap<String, String> =
        mutableMapOf(
            "quarkus.hibernate-orm.enabled" to "false",
            "quarkus.flyway.migrate-at-start" to "false",
            "quarkus.datasource.db-kind" to "h2",
            "quarkus.datasource.jdbc" to "false",
            "quarkus.datasource.active" to "false",
            "quarkus.datasource.devservices.enabled" to "false",
            "quarkus.datasource.reactive" to "false",
            "quarkus.datasource.jdbc.url" to "jdbc:h2:mem:ar-command-validation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "quarkus.datasource.username" to "sa",
            "quarkus.datasource.password" to "sa",
            "quarkus.arc.exclude-types" to EXCLUDED_TYPES,
        )

    companion object {
        private val EXCLUDED_TYPES =
            listOf(
                "com.erp.financial.ar.application.service.*",
                "com.erp.financial.ar.infrastructure.adapter.output.*",
                "com.erp.financial.ar.infrastructure.adapter.output.persistence.*",
            ).joinToString(",")
    }
}
