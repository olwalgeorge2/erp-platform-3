package com.erp.financial.ar.infrastructure.adapter.input.rest

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
@TestProfile(ArCommandValidationTestProfile::class)
class CustomerResourceValidationTest {
    @Test
    fun `delete customer uses spanish validation envelope`() {
        given()
            .header("Accept-Language", "es-ES")
            .queryParam("tenantId", TENANT_ID)
            .`when`()
            .delete("/api/v1/finance/customers/not-a-uuid")
            .then()
            .statusCode(422)
            .body("code", equalTo("FINANCE_INVALID_CUSTOMER_ID"))
            .body("message", equalTo("El identificador del cliente 'not-a-uuid' no es válido."))
            .body("validationErrors[0].field", equalTo("customerId"))
            .body("validationErrors[0].code", equalTo("FINANCE_INVALID_CUSTOMER_ID"))
            .body("validationErrors[0].message", equalTo("El identificador del cliente 'not-a-uuid' no es válido."))
            .body("validationErrors[0].rejectedValue", equalTo("not-a-uuid"))
    }

    @Test
    fun `create customer surfaces spanish validation error`() {
        val payload =
            """
            {
              "tenantId": "$TENANT_ID",
              "companyCodeId": "$COMPANY_CODE_ID",
              "customerNumber": "C-1000",
              "name": "   ",
              "currency": "USD",
              "paymentTerms": {
                "code": "NET30",
                "type": "NET",
                "dueInDays": 30
              },
              "billingAddress": {
                "line1": "Calle Test 1",
                "city": "Madrid",
                "countryCode": "ES"
              }
            }
            """.trimIndent()

        val expected = "El campo 'name' no puede estar vac\u00EDo."

        given()
            .header("Accept-Language", "es-ES")
            .contentType("application/json")
            .body(payload)
            .`when`()
            .post("/api/v1/finance/customers")
            .then()
            .statusCode(422)
            .body("code", equalTo("FINANCE_INVALID_NAME"))
            .body("message", equalTo(expected))
            .body("validationErrors[0].field", equalTo("name"))
            .body("validationErrors[0].code", equalTo("FINANCE_INVALID_NAME"))
            .body("validationErrors[0].message", equalTo(expected))
            .body("validationErrors[0].rejectedValue", equalTo("   "))
    }

    companion object {
        private const val TENANT_ID = "99999999-1111-2222-3333-444444444444"
        private const val COMPANY_CODE_ID = "aaaaaaaa-ffff-eeee-dddd-cccccccccccc"
    }
}
