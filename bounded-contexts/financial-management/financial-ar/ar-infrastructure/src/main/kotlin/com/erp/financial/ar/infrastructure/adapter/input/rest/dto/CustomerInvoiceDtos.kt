package com.erp.financial.ar.infrastructure.adapter.input.rest.dto

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.ar.application.port.input.command.CreateCustomerInvoiceCommand
import com.erp.financial.ar.application.port.input.command.PostCustomerInvoiceCommand
import com.erp.financial.ar.application.port.input.command.RecordCustomerReceiptCommand
import com.erp.financial.ar.application.port.input.query.ListCustomerInvoicesQuery
import com.erp.financial.ar.domain.model.invoice.CustomerInvoice
import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceStatus
import com.erp.financial.shared.masterdata.PaymentTermType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateCustomerInvoiceRequest(
    @field:NotNull val tenantId: UUID,
    @field:NotNull val companyCodeId: UUID,
    @field:NotNull val customerId: UUID,
    @field:NotBlank val invoiceNumber: String,
    @field:NotNull val invoiceDate: LocalDate,
    @field:NotNull val dueDate: LocalDate,
    @field:NotBlank val currency: String,
    val dimensionDefaults: InvoiceDimensionRequest? = null,
    @field:NotEmpty val lines: List<CustomerInvoiceLineRequest>,
)

data class CustomerInvoiceLineRequest(
    @field:NotNull val glAccountId: UUID,
    @field:NotBlank val description: String,
    @field:Positive val netAmount: Long,
    @field:PositiveOrZero val taxAmount: Long = 0,
    val dimensionOverrides: InvoiceDimensionRequest? = null,
)

data class CustomerInvoiceResponse(
    val id: UUID,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val customerId: UUID,
    val invoiceNumber: String,
    val invoiceDate: LocalDate,
    val dueDate: LocalDate,
    val currency: String,
    val netAmount: Long,
    val taxAmount: Long,
    val receivedAmount: Long,
    val status: CustomerInvoiceStatus,
    val dimensionDefaults: InvoiceDimensionResponse,
    val lines: List<CustomerInvoiceLineResponse>,
    val paymentTerms: PaymentTermsDto,
    val createdAt: Instant,
    val updatedAt: Instant,
    val postedAt: Instant?,
    val journalEntryId: UUID?,
)

data class CustomerInvoiceLineResponse(
    val id: UUID,
    val glAccountId: UUID,
    val description: String,
    val netAmount: Long,
    val taxAmount: Long,
    val dimensionAssignments: InvoiceDimensionResponse,
)

data class PostCustomerInvoiceRequest(
    @field:NotNull val tenantId: UUID,
)

data class ReceiptRequest(
    @field:NotNull val tenantId: UUID,
    @field:Positive val amount: Long,
    val receiptDate: LocalDate? = null,
)

data class CustomerInvoiceSearchRequest(
    @field:NotNull val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val customerId: UUID? = null,
    val status: CustomerInvoiceStatus? = null,
    val dueBefore: LocalDate? = null,
)

data class PaymentTermsDto(
    val code: String,
    val type: PaymentTermType,
    val dueInDays: Int,
    val discountPercentage: BigDecimal? = null,
    val discountDays: Int? = null,
)

data class InvoiceDimensionRequest(
    val costCenterId: UUID? = null,
    val profitCenterId: UUID? = null,
    val departmentId: UUID? = null,
    val projectId: UUID? = null,
    val businessAreaId: UUID? = null,
)

data class InvoiceDimensionResponse(
    val costCenterId: UUID? = null,
    val profitCenterId: UUID? = null,
    val departmentId: UUID? = null,
    val projectId: UUID? = null,
    val businessAreaId: UUID? = null,
)

fun CreateCustomerInvoiceRequest.toCommand(): CreateCustomerInvoiceCommand =
    CreateCustomerInvoiceCommand(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        customerId = customerId,
        invoiceNumber = invoiceNumber,
        invoiceDate = invoiceDate,
        dueDate = dueDate,
        currency = currency,
        lines =
            lines.map {
                CreateCustomerInvoiceCommand.Line(
                    glAccountId = it.glAccountId,
                    description = it.description,
                    netAmount = it.netAmount,
                    taxAmount = it.taxAmount,
                    dimensionAssignments = it.dimensionOverrides.toAssignments(),
                )
            },
        dimensionAssignments = dimensionDefaults.toAssignments(),
    )

fun PostCustomerInvoiceRequest.toCommand(invoiceId: UUID): PostCustomerInvoiceCommand =
    PostCustomerInvoiceCommand(
        tenantId = tenantId,
        invoiceId = invoiceId,
    )

fun ReceiptRequest.toCommand(invoiceId: UUID): RecordCustomerReceiptCommand =
    RecordCustomerReceiptCommand(
        tenantId = tenantId,
        invoiceId = invoiceId,
        receiptAmount = amount,
        receiptDate = receiptDate,
    )

fun CustomerInvoiceSearchRequest.toQuery(): ListCustomerInvoicesQuery =
    ListCustomerInvoicesQuery(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        customerId = customerId,
        status = status,
        dueBefore = dueBefore,
    )

fun CustomerInvoice.toResponse(): CustomerInvoiceResponse =
    CustomerInvoiceResponse(
        id = id.value,
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        customerId = customerId,
        invoiceNumber = invoiceNumber,
        invoiceDate = invoiceDate,
        dueDate = dueDate,
        currency = currency,
        netAmount = netAmount.amount,
        taxAmount = taxAmount.amount,
        receivedAmount = receivedAmount.amount,
        status = status,
        dimensionDefaults =
            InvoiceDimensionResponse(
                costCenterId = dimensionAssignments.costCenterId,
                profitCenterId = dimensionAssignments.profitCenterId,
                departmentId = dimensionAssignments.departmentId,
                projectId = dimensionAssignments.projectId,
                businessAreaId = dimensionAssignments.businessAreaId,
            ),
        lines =
            lines.map {
                CustomerInvoiceLineResponse(
                    id = it.id,
                    glAccountId = it.glAccountId,
                    description = it.description,
                    netAmount = it.netAmount.amount,
                    taxAmount = it.taxAmount.amount,
                    dimensionAssignments =
                        InvoiceDimensionResponse(
                            costCenterId = it.dimensionAssignments.costCenterId,
                            profitCenterId = it.dimensionAssignments.profitCenterId,
                            departmentId = it.dimensionAssignments.departmentId,
                            projectId = it.dimensionAssignments.projectId,
                            businessAreaId = it.dimensionAssignments.businessAreaId,
                        ),
                )
            },
        paymentTerms =
            PaymentTermsDto(
                code = paymentTerms.code,
                type = paymentTerms.type,
                dueInDays = paymentTerms.dueInDays,
                discountPercentage = paymentTerms.discountPercentage,
                discountDays = paymentTerms.discountDays,
            ),
        createdAt = createdAt,
        updatedAt = updatedAt,
        postedAt = postedAt,
        journalEntryId = journalEntryId,
    )

private fun InvoiceDimensionRequest?.toAssignments(): DimensionAssignments =
    this?.let {
        DimensionAssignments(
            costCenterId = it.costCenterId,
            profitCenterId = it.profitCenterId,
            departmentId = it.departmentId,
            projectId = it.projectId,
            businessAreaId = it.businessAreaId,
        )
    } ?: DimensionAssignments()
