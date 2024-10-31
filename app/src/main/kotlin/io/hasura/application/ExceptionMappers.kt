package io.hasura.application

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import gdc.ir.ErrorResponse
import gdc.sqlgen.generic.MutationPermissionCheckFailureException
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.Logger

@Provider
class GenericExceptionMapper : ExceptionHandler(), jakarta.ws.rs.ext.ExceptionMapper<Throwable> {
    override fun toResponse(exception: Throwable): Response {
        println("==== GENERIC EXCEPTION MAPPER CALLED ====")
        return handleExceptions(exception, Response.Status.INTERNAL_SERVER_ERROR)
    }
}



abstract class ExceptionHandler {

    @Inject
    private lateinit var logger: Logger

    fun handleExceptions(
        exception: Throwable,
        status: Response.Status,
        message: String? = null
    ): Response {
        try {
            return Response
                .status(status)
                .entity(
                    ErrorResponse(
                        message = message ?: exception.message ?: "An uncaught error occurred",
                        details = mapOf("stacktrace" to exception.stackTraceToString())
                    )
                )
                .build()
        } finally {
            logger.error("Uncaught exception", exception)
        }
    }
}
