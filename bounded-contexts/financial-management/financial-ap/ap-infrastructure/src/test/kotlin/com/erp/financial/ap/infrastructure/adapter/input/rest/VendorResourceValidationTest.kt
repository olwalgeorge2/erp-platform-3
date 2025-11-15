package com.erp.financial.ap.infrastructure.adapter.input.rest

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
@TestProfile(ApCommandValidationTestProfile::class)
class VendorResourceValidationTest {
    @Test
    fun `delete vendor uses spanish validation envelope`() {
        given()
            .header("Accept-Language", "es-ES")
            .queryParam("tenantId", TENANT_ID)
            .`when`()
            .delete("/api/v1/finance/vendors/not-a-uuid")
            .then()
            .statusCode(422)
            .body("code", equalTo("FINANCE_INVALID_VENDOR_ID"))
            .body("message", equalTo("El identificador del proveedor 'not-a-uuid' no es válido."))
            .body("validationErrors[0].field", equalTo("vendorId"))
            .body("validationErrors[0].code", equalTo("FINANCE_INVALID_VENDOR_ID"))
            .body("validationErrors[0].message", equalTo("El identificador del proveedor 'not-a-uuid' no es válido."))
            .body("validationErrors[0].rejectedValue", equalTo("not-a-uuid"))
    }

    @Test
    fun `create vendor returns german currency error`() {
        val payload =
            """
            {
              "tenantId": "$TENANT_ID",
              "companyCodeId": "$COMPANY_CODE_ID",
              "vendorNumber": "V-12345",
              "name": "Bad Currency Vendor",
              "currency": "usdollars",
              "paymentTerms": {
                "code": "NET30",
                "type": "NET",
                "dueInDays": 30
              },
              "address": {
                "line1": "Invalid Str. 1",
                "city": "Berlin",
                "countryCode": "DE"
              }
            }
            """.trimIndent()

        val expectedMessage =
            "Die W\u00E4hrung 'usdollars' ist ung\u00FCltig. Erwartet wird ein dreistelliger ISO-Code."

        given()
            .header("Accept-Language", "de-DE")
            .contentType("application/json")
            .body(payload)
            .`when`()
            .post("/api/v1/finance/vendors")
            .then()
            .statusCode(422)
            .body("code", equalTo("FINANCE_INVALID_CURRENCY_CODE"))
            .body("message", equalTo(expectedMessage))
            .body("validationErrors[0].field", equalTo("currency"))
            .body("validationErrors[0].code", equalTo("FINANCE_INVALID_CURRENCY_CODE"))
            .body("validationErrors[0].message", equalTo(expectedMessage))
            .body("validationErrors[0].rejectedValue", equalTo("usdollars"))
    }

    companion object {
        private const val TENANT_ID = "bbbbbbbb-9999-4444-aaaa-cccccccccccc"
        private const val COMPANY_CODE_ID = "aaaaaaaa-1111-2222-3333-444444444444"
    }
}
