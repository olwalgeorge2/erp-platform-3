package com.erp.finance.accounting.infrastructure.adapter.input.rest

import com.erp.finance.accounting.application.port.input.FinanceCommandUseCase
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.ClosePeriodRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.CreateLedgerRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.DefineAccountRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.ErrorResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.PostJournalEntryRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.toResponse
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.util.UUID

@ApplicationScoped
@Path("/api/v1/finance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Finance", description = "Financial accounting command endpoints")
class FinanceCommandResource(
    private val commandService: FinanceCommandUseCase,
) {
    @POST
    @Path("/ledgers")
    @Operation(summary = "Create a ledger")
    fun createLedger(request: CreateLedgerRequest): Response =
        execute {
            commandService
                .createLedger(request.toCommand())
                .toResponse()
        }.created()

    @POST
    @Path("/chart-of-accounts/{chartId}/accounts")
    @Operation(summary = "Define an account in the chart of accounts")
    fun defineAccount(
        @PathParam("chartId") chartId: String,
        request: DefineAccountRequest,
    ): Response =
        execute {
            commandService
                .defineAccount(
                    request.toCommand(
                        chartOfAccountsId = UUID.fromString(chartId),
                    ),
                ).toResponse()
        }.ok()

    @POST
    @Path("/journal-entries")
    @Operation(summary = "Post a journal entry")
    fun postJournalEntry(request: PostJournalEntryRequest): Response =
        execute {
            commandService
                .postJournalEntry(request.toCommand())
                .toResponse()
        }.accepted()

    @POST
    @Path("/ledgers/{ledgerId}/periods/{periodId}/close")
    @Operation(summary = "Close or freeze an accounting period")
    fun closePeriod(
        @PathParam("ledgerId") ledgerId: String,
        @PathParam("periodId") periodId: String,
        request: ClosePeriodRequest,
    ): Response =
        execute {
            commandService
                .closePeriod(
                    request.toCommand(
                        ledgerId = UUID.fromString(ledgerId),
                        periodId = UUID.fromString(periodId),
                    ),
                ).toResponse()
        }.ok()

    private fun <T> execute(block: () -> T): CommandResult<T> =
        runCatching { block() }
            .fold(
                onSuccess = { CommandResult.Success(it) },
                onFailure = { CommandResult.Failure(it) },
            )

    private fun <T> CommandResult<T>.created(): Response =
        when (this) {
            is CommandResult.Success -> Response.status(Response.Status.CREATED).entity(value).build()
            is CommandResult.Failure -> toResponse()
        }

    private fun <T> CommandResult<T>.accepted(): Response =
        when (this) {
            is CommandResult.Success -> Response.status(Response.Status.ACCEPTED).entity(value).build()
            is CommandResult.Failure -> toResponse()
        }

    private fun <T> CommandResult<T>.ok(): Response =
        when (this) {
            is CommandResult.Success -> Response.ok(value).build()
            is CommandResult.Failure -> toResponse()
        }

    private fun CommandResult.Failure.toResponse(): Response {
        val (status, code) =
            when (error) {
                is IllegalArgumentException -> Response.Status.BAD_REQUEST to "INVALID_REQUEST"
                is IllegalStateException -> Response.Status.NOT_FOUND to "RESOURCE_NOT_FOUND"
                else -> Response.Status.INTERNAL_SERVER_ERROR to "FINANCE_COMMAND_ERROR"
            }

        val payload =
            ErrorResponse(
                code = code,
                message = error.message ?: "Finance command failed",
            )
        return Response.status(status).entity(payload).build()
    }

    private sealed interface CommandResult<out T> {
        data class Success<T>(
            val value: T,
        ) : CommandResult<T>

        data class Failure(
            val error: Throwable,
        ) : CommandResult<Nothing>
    }
}
