package com.segurosbolivar.policy.api;

import com.segurosbolivar.policy.domain.DomainRuleViolationException;
import com.segurosbolivar.policy.service.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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
            IllegalArgumentException.class
    })
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

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> concurrentUpdate(
            OptimisticLockingFailureException exception,
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

