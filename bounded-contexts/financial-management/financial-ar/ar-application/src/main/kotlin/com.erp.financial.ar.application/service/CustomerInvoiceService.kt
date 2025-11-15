package com.erp.financial.ar.application.service

import com.erp.financial.ar.application.port.input.CustomerInvoiceUseCase
import com.erp.financial.ar.application.port.input.command.CreateCustomerInvoiceCommand
import com.erp.financial.ar.application.port.input.command.PostCustomerInvoiceCommand
import com.erp.financial.ar.application.port.input.command.RecordCustomerReceiptCommand
import com.erp.financial.ar.application.port.input.query.CustomerInvoiceDetailQuery
import com.erp.financial.ar.application.port.input.query.ListCustomerInvoicesQuery
import com.erp.financial.ar.application.port.output.ArOpenItemRepository
import com.erp.financial.ar.application.port.output.CustomerInvoiceRepository
import com.erp.financial.ar.application.port.output.CustomerRepository
import com.erp.financial.ar.application.port.output.ReceivablePostingPort
import com.erp.financial.ar.domain.model.customer.CustomerId
import com.erp.financial.ar.domain.model.invoice.CustomerInvoice
import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceId
import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceLine
import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceStatus
import com.erp.financial.shared.Money
import jakarta.enterprise.context.ApplicationScoped
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
class CustomerInvoiceService
    @jakarta.inject.Inject
    constructor(
        private val invoiceRepository: CustomerInvoiceRepository,
        private val customerRepository: CustomerRepository,
        private val postingPort: ReceivablePostingPort,
        private val openItemRepository: ArOpenItemRepository,
        private val clock: Clock,
    ) : CustomerInvoiceUseCase {
        override fun createInvoice(command: CreateCustomerInvoiceCommand): CustomerInvoice {
            val customer =
                customerRepository.findById(command.tenantId, CustomerId(command.customerId))
                    ?: throw IllegalArgumentException("Customer not found")
            val currency = command.currency.uppercase()
            val headerDimensions =
                if (command.dimensionAssignments.isEmpty() && !customer.profile.dimensionDefaults.isEmpty()) {
                    customer.profile.dimensionDefaults
                } else {
                    command.dimensionAssignments
                }
            val lines =
                command.lines.map {
                    CustomerInvoiceLine(
                        glAccountId = it.glAccountId,
                        description = it.description,
                        netAmount = Money(it.netAmount, currency),
                        taxAmount = Money(it.taxAmount, currency),
                        dimensionAssignments = it.dimensionAssignments,
                    )
                }
            val invoice =
                CustomerInvoice.draft(
                    tenantId = command.tenantId,
                    companyCodeId = command.companyCodeId,
                    customerId = command.customerId,
                    invoiceNumber = command.invoiceNumber,
                    invoiceDate = command.invoiceDate,
                    dueDate = command.dueDate,
                    currency = currency,
                    lines = lines,
                    dimensionAssignments = headerDimensions,
                    taxAmount = Money(command.lines.sumOf { it.taxAmount }, currency),
                    paymentTerms = customer.profile.paymentTerms,
                    clock = clock,
                )
            val approved = invoice.approve(clock)
            return invoiceRepository.save(approved)
        }

        override fun postInvoice(command: PostCustomerInvoiceCommand): CustomerInvoice {
            val invoice = loadInvoice(command.tenantId, command.invoiceId)
            if (invoice.status == CustomerInvoiceStatus.POSTED && invoice.journalEntryId != null) {
                return invoice
            }
            val posted = invoice.post(clock)
            val postingResult = postingPort.postCustomerInvoice(posted)
            val withJournal = posted.assignJournalEntry(postingResult.journalEntryId, clock)
            return invoiceRepository.save(withJournal)
        }

        override fun recordReceipt(command: RecordCustomerReceiptCommand): CustomerInvoice {
            val invoice = loadInvoice(command.tenantId, command.invoiceId)
            val paymentDate = command.receiptDate ?: LocalDate.now(clock)
            val updated = invoice.applyReceipt(Money(command.receiptAmount, invoice.currency), clock)
            val saved = invoiceRepository.save(updated)
            openItemRepository.updateReceiptMetadata(saved.id.value, paymentDate)
            return saved
        }

        override fun listInvoices(query: ListCustomerInvoicesQuery): List<CustomerInvoice> =
            invoiceRepository.list(query.tenantId, query.companyCodeId, query.customerId, query.status, query.dueBefore)

        override fun getInvoice(query: CustomerInvoiceDetailQuery): CustomerInvoice? =
            invoiceRepository.findById(query.tenantId, CustomerInvoiceId(query.invoiceId))

        private fun loadInvoice(
            tenantId: UUID,
            invoiceId: UUID,
        ): CustomerInvoice =
            invoiceRepository.findById(tenantId, CustomerInvoiceId(invoiceId))
                ?: throw IllegalArgumentException("Invoice not found")
    }
