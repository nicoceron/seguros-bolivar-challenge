// Purpose of this file: Turns validation, business-rule, and concurrency errors into clear HTTP responses.
package com.segurosbolivar.policy.api;

import com.segurosbolivar.policy.domain.DomainRuleViolationException;
import com.segurosbolivar.policy.service.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import java.time.DateTimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    /** Returns HTTP 404 with a readable code when an ID does not exist. */
    ResponseEntity<ProblemDetail> notFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                exception.getCode(),
                exception.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(DomainRuleViolationException.class)
    /** Returns HTTP 409 when a policy rule rejects an operation. */
    ResponseEntity<ProblemDetail> domainRule(
            DomainRuleViolationException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                exception.getCode(),
                exception.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    /** Collects invalid JSON fields and returns HTTP 400. */
    ResponseEntity<ProblemDetail> bodyValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return problem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "The request body is invalid",
                request.getRequestURI(),
                errors
        );
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class,
            DateTimeException.class
    })
    /** Returns HTTP 400 when a request cannot be parsed or validated. */
    ResponseEntity<ProblemDetail> invalidRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "The request could not be parsed or validated",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    /** Returns HTTP 409 when another request changed or locked the record. */
    ResponseEntity<ProblemDetail> concurrentUpdate(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "CONCURRENT_UPDATE",
                "The resource changed during this request; retrieve it and retry",
                request.getRequestURI(),
                null
        );
    }

    /** Builds the shared error body with code, request path, and time. */
    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String code,
            String detail,
            String path,
            Map<String, String> errors
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("https://github.com/nicoceron/seguros-bolivar-challenge/problems/" + code));
        problem.setProperty("code", code);
        problem.setProperty("path", path);
        problem.setProperty("timestamp", Instant.now());
        if (errors != null && !errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return ResponseEntity.status(status).body(problem);
    }
}


