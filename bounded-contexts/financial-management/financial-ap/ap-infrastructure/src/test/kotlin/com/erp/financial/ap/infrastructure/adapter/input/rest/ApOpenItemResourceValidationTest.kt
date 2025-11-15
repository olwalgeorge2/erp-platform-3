package com.erp.financial.ap.infrastructure.adapter.input.rest

import com.erp.financial.ap.application.port.input.query.ApAgingQuery
import com.erp.financial.ap.application.port.input.query.ApOpenItemQueryUseCase
import io.quarkus.test.Mock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import jakarta.enterprise.context.ApplicationScoped
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
@TestProfile(ApValidationTestProfile::class)
class ApOpenItemResourceValidationTest {
    @Test
    fun `invalid asOfDate uses shared envelope in english`() {
        given()
            .header("Accept-Language", "en-US")
            .queryParam("tenantId", TENANT_ID)
            .queryParam("asOfDate", "2024-13-01")
            .`when`()
            .get("/api/v1/finance/ap/open-items/aging/detail")
            .then()
            .statusCode(422)
            .body("code", equalTo("FINANCE_INVALID_DATE"))
            .body("message", equalTo("Date '2024-13-01' is invalid. Expected format yyyy-MM-dd."))
            .body("validationErrors[0].field", equalTo("asOfDate"))
            .body("validationErrors[0].code", equalTo("FINANCE_INVALID_DATE"))
            .body("validationErrors[0].message", equalTo("Date '2024-13-01' is invalid. Expected format yyyy-MM-dd."))
            .body("validationErrors[0].rejectedValue", equalTo("2024-13-01"))
    }

    companion object {
        private const val TENANT_ID = "00000000-0000-0000-0000-000000000123"
    }

    @Mock
    @ApplicationScoped
    class NoopApOpenItemQueryUseCase : ApOpenItemQueryUseCase {
        override fun getAgingDetail(query: ApAgingQuery) =
            throw UnsupportedOperationException("Should not be invoked during validation tests.")

        override fun getAgingSummary(query: ApAgingQuery) =
            throw UnsupportedOperationException("Should not be invoked during validation tests.")
    }
}
