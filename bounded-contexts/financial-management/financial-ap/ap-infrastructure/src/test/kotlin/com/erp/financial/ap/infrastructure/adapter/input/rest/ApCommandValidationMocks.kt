package com.erp.financial.ap.infrastructure.adapter.input.rest

import com.erp.financial.ap.application.port.input.BillCommandUseCase
import com.erp.financial.ap.application.port.input.PaymentProposalUseCase
import com.erp.financial.ap.application.port.input.VendorCommandUseCase
import com.erp.financial.ap.application.port.input.command.CreateVendorBillCommand
import com.erp.financial.ap.application.port.input.command.GeneratePaymentProposalCommand
import com.erp.financial.ap.application.port.input.command.PostVendorBillCommand
import com.erp.financial.ap.application.port.input.command.RecordVendorPaymentCommand
import com.erp.financial.ap.application.port.input.command.RegisterVendorCommand
import com.erp.financial.ap.application.port.input.command.UpdateVendorCommand
import com.erp.financial.ap.application.port.input.command.UpdateVendorStatusCommand
import com.erp.financial.ap.application.port.input.query.ListVendorBillsQuery
import com.erp.financial.ap.application.port.input.query.ListVendorsQuery
import com.erp.financial.ap.application.port.input.query.VendorBillDetailQuery
import com.erp.financial.ap.application.port.input.query.VendorDetailQuery
import com.erp.financial.ap.application.port.output.ListPaymentProposalsQuery
import com.erp.financial.ap.domain.model.bill.VendorBill
import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposal
import com.erp.financial.ap.domain.model.vendor.Vendor
import io.quarkus.test.Mock
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@Mock
@ApplicationScoped
class MockBillCommandUseCase : BillCommandUseCase {
    override fun createBill(command: CreateVendorBillCommand): VendorBill =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun postBill(command: PostVendorBillCommand): VendorBill =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun recordPayment(command: RecordVendorPaymentCommand): VendorBill =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun listBills(query: ListVendorBillsQuery): List<VendorBill> =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun getBill(query: VendorBillDetailQuery): VendorBill? =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")
}

@Mock
@ApplicationScoped
class MockPaymentProposalUseCase : PaymentProposalUseCase {
    override fun generateProposal(command: GeneratePaymentProposalCommand): PaymentProposal =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun listProposals(query: ListPaymentProposalsQuery): List<PaymentProposal> =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun getProposal(
        tenantId: UUID,
        proposalId: UUID,
    ): PaymentProposal? = throw UnsupportedOperationException("Should not be invoked during validation tests.")
}

@Mock
@ApplicationScoped
class MockVendorCommandUseCase : VendorCommandUseCase {
    override fun registerVendor(command: RegisterVendorCommand): Vendor =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun updateVendor(command: UpdateVendorCommand): Vendor =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun updateVendorStatus(command: UpdateVendorStatusCommand): Vendor =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun listVendors(query: ListVendorsQuery): List<Vendor> =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun getVendor(query: VendorDetailQuery): Vendor? =
        throw UnsupportedOperationException("Should not be invoked during validation tests.")

    override fun deleteVendor(
        tenantId: UUID,
        vendorId: UUID,
    ): Unit = throw UnsupportedOperationException("Should not be invoked during validation tests.")
}
