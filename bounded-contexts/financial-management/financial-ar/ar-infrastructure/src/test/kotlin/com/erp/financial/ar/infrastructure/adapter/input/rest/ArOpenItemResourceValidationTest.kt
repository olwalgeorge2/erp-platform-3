package com.erp.financial.ar.infrastructure.adapter.input.rest

import com.erp.financial.ar.application.port.input.query.ArAgingQuery
import com.erp.financial.ar.application.port.input.query.ArOpenItemQueryUseCase
import io.quarkus.test.Mock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import jakarta.enterprise.context.ApplicationScoped
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
@TestProfile(ArValidationTestProfile::class)
class ArOpenItemResourceValidationTest {
    @Test
    fun `invalid asOfDate honors accept language`() {
        given()
            .header("Accept-Language", "es-ES")
            .queryParam("tenantId", TENANT_ID)
            .queryParam("asOfDate", "2024-13-15")
            .`when`()
            .get("/api/v1/finance/ar/open-items/aging/summary")
            .then()
            .statusCode(422)
            .body("code", equalTo("FINANCE_INVALID_DATE"))
            .body("validationErrors[0].message", equalTo("La fecha '2024-13-15' no es v\u00E1lida. Formato esperado: yyyy-MM-dd."))
            .body("validationErrors[0].rejectedValue", equalTo("2024-13-15"))
    }

    companion object {
        private const val TENANT_ID = "00000000-0000-0000-0000-000000000321"
    }

    @Mock
    @ApplicationScoped
    class NoopArOpenItemQueryUseCase : ArOpenItemQueryUseCase {
        override fun getAgingDetail(query: ArAgingQuery) =
            throw UnsupportedOperationException("Should not be invoked during validation tests.")

        override fun getAgingSummary(query: ArAgingQuery) =
            throw UnsupportedOperationException("Should not be invoked during validation tests.")
    }
}
