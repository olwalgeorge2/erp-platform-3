package com.erp.financial.ap.infrastructure.adapter.input.rest.dto

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.ap.application.port.input.command.CreateVendorBillCommand
import com.erp.financial.ap.application.port.input.command.PostVendorBillCommand
import com.erp.financial.ap.application.port.input.command.RecordVendorPaymentCommand
import com.erp.financial.ap.application.port.input.query.ListVendorBillsQuery
import com.erp.financial.ap.domain.model.bill.BillStatus
import com.erp.financial.ap.domain.model.bill.VendorBill
import com.erp.financial.shared.masterdata.PaymentTermType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateBillRequest(
    val tenantId: UUID,
    val companyCodeId: UUID,
    val vendorId: UUID,
    val invoiceNumber: String,
    val invoiceDate: LocalDate,
    val dueDate: LocalDate,
    val currency: String,
    val dimensionDefaults: DimensionDefaultsRequest? = null,
    val lines: List<BillLineRequest>,
)

data class BillLineRequest(
    val glAccountId: UUID,
    val description: String,
    val netAmount: Long,
    val taxAmount: Long = 0,
    val dimensionOverrides: DimensionDefaultsRequest? = null,
)

data class BillResponse(
    val id: UUID,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val vendorId: UUID,
    val invoiceNumber: String,
    val invoiceDate: LocalDate,
    val dueDate: LocalDate,
    val currency: String,
    val netAmount: Long,
    val taxAmount: Long,
    val paidAmount: Long,
    val status: BillStatus,
    val dimensionDefaults: DimensionDefaultsRequest,
    val lines: List<BillLineResponse>,
    val paymentTerms: PaymentTermsDto,
    val createdAt: Instant,
    val updatedAt: Instant,
    val postedAt: Instant?,
    val journalEntryId: UUID?,
)

data class BillLineResponse(
    val id: UUID,
    val glAccountId: UUID,
    val description: String,
    val netAmount: Long,
    val taxAmount: Long,
    val dimensionAssignments: DimensionDefaultsRequest,
)

data class PostBillRequest(
    val tenantId: UUID,
)

data class PaymentRequest(
    val tenantId: UUID,
    val amount: Long,
    val paymentDate: LocalDate? = null,
)

data class BillSearchRequest(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val vendorId: UUID? = null,
    val status: BillStatus? = null,
    val dueBefore: LocalDate? = null,
)

fun CreateBillRequest.toCommand(): CreateVendorBillCommand =
    CreateVendorBillCommand(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        vendorId = vendorId,
        invoiceNumber = invoiceNumber,
        invoiceDate = invoiceDate,
        dueDate = dueDate,
        currency = currency,
        lines =
            lines.map {
                CreateVendorBillCommand.Line(
                    glAccountId = it.glAccountId,
                    description = it.description,
                    netAmount = it.netAmount,
                    taxAmount = it.taxAmount,
                    dimensionAssignments = it.dimensionOverrides.toAssignments(),
                )
            },
        dimensionAssignments = dimensionDefaults.toAssignments(),
    )

fun PostBillRequest.toCommand(billId: UUID): PostVendorBillCommand =
    PostVendorBillCommand(
        tenantId = tenantId,
        billId = billId,
    )

fun PaymentRequest.toCommand(billId: UUID): RecordVendorPaymentCommand =
    RecordVendorPaymentCommand(
        tenantId = tenantId,
        billId = billId,
        paymentAmount = amount,
        paymentDate = paymentDate,
    )

fun BillSearchRequest.toQuery(): ListVendorBillsQuery =
    ListVendorBillsQuery(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        vendorId = vendorId,
        status = status,
        dueBefore = dueBefore,
    )

fun VendorBill.toResponse(): BillResponse =
    BillResponse(
        id = id.value,
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        vendorId = vendorId,
        invoiceNumber = invoiceNumber,
        invoiceDate = invoiceDate,
        dueDate = dueDate,
        currency = currency,
        netAmount = netAmount.amount,
        taxAmount = taxAmount.amount,
        paidAmount = paidAmount.amount,
        status = status,
        dimensionDefaults =
            DimensionDefaultsRequest(
                costCenterId = dimensionAssignments.costCenterId,
                profitCenterId = dimensionAssignments.profitCenterId,
                departmentId = dimensionAssignments.departmentId,
                projectId = dimensionAssignments.projectId,
                businessAreaId = dimensionAssignments.businessAreaId,
            ),
        lines =
            lines.map {
                BillLineResponse(
                    id = it.id,
                    glAccountId = it.glAccountId,
                    description = it.description,
                    netAmount = it.netAmount.amount,
                    taxAmount = it.taxAmount.amount,
                    dimensionAssignments =
                        DimensionDefaultsRequest(
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

private fun DimensionDefaultsRequest?.toAssignments(): DimensionAssignments =
    this?.let {
        DimensionAssignments(
            costCenterId = it.costCenterId,
            profitCenterId = it.profitCenterId,
            departmentId = it.departmentId,
            projectId = it.projectId,
            businessAreaId = it.businessAreaId,
        )
    } ?: DimensionAssignments()
data class PaymentTermsDto(
    val code: String,
    val type: PaymentTermType,
    val dueInDays: Int,
    val discountPercentage: BigDecimal? = null,
    val discountDays: Int? = null,
)
