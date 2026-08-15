package com.edgareldy.springmicroservicestutorial.commonlib.exception;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Base set of exception mappings shared by every business service's own
 * {@code @RestControllerAdvice}. This class deliberately carries no
 * {@code @RestControllerAdvice}/{@code @ControllerAdvice} annotation itself:
 * Spring only activates {@code @ExceptionHandler} methods declared on a bean
 * that is itself advice-annotated, so each service must extend this class and
 * annotate its own concrete subclass with {@code @RestControllerAdvice}. That
 * subclass can then add service-specific {@code @ExceptionHandler} cases on
 * top of the ones inherited here.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public class BaseExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BaseExceptionHandler.class);

    /**
     * Maps a missing resource to a 404, wrapping the exception message in
     * the standard {@link ApiResponse} envelope.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.debug("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Maps a domain rule violation to a 400. Services that need a different
     * status for a specific case (e.g. 409 on a conflicting update) are free
     * to declare their own, more specific {@code @ExceptionHandler} for that
     * case in their concrete subclass; Spring resolves the most specific
     * match first.
     */
    @ExceptionHandler(BusinessRuleException.class)
    protected ResponseEntity<ApiResponse<Object>> handleBusinessRule(BusinessRuleException ex) {
        log.debug("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Catch-all for any exception not covered by a more specific handler,
     * logged at ERROR since it represents an unanticipated failure.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}
