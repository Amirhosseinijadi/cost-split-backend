package com.costsplit.api.api

import com.costsplit.api.service.ConflictException
import com.costsplit.api.service.InvalidRequestException
import com.costsplit.api.service.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(exception: NotFoundException): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, "Resource not found", exception.message)

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(exception: ConflictException): ProblemDetail =
        problem(HttpStatus.CONFLICT, "Conflict", exception.message)

    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequest(exception: InvalidRequestException): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.message)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ProblemDetail {
        val errors = exception.bindingResult.fieldErrors
            .groupBy({ it.field }, { it.defaultMessage ?: "is invalid" })
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid")
            .also { it.setProperty("errors", errors) }
    }

    private fun problem(status: HttpStatus, title: String, detail: String?): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail ?: title).also { it.title = title }
}
