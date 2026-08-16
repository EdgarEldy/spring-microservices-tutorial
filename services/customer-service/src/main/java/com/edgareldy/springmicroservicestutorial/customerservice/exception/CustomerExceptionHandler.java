package com.edgareldy.springmicroservicestutorial.customerservice.exception;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.BaseExceptionHandler;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Activates {@link BaseExceptionHandler}'s {@code ResourceNotFoundException}/
 * {@code BusinessRuleException}/catch-all mappings for {@code customer-service}, and adds
 * the same three mappings {@code catalog-service}'s {@code CatalogExceptionHandler} needed:
 * {@code common-lib} has no handler for a {@code @Valid} failure or a security-filter-level
 * denial, so without these methods here they would both fall through to the catch-all 500.
 * This service has no {@code MethodSecurityConfig} ({@code authenticated()} is enforced
 * entirely in {@code SecurityConfig}), so {@link AccessDeniedException} is only reachable
 * here via its {@code AuthorizationDeniedException} subtype, kept anyway for parity with
 * every other service's exception handler.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@RestControllerAdvice
public class CustomerExceptionHandler extends BaseExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomerExceptionHandler.class);

    /**
     * Maps a {@code @Valid} request body failure to a 400, collecting every field error into
     * one readable message rather than letting it fall through to the catch-all 500.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.debug("Validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    /**
     * Maps an access-control denial to a 403. Also covers
     * {@code AuthorizationDeniedException}, which extends this class.
     */
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        log.debug("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied"));
    }

    /**
     * Maps a fully unauthenticated request denied by {@code SecurityConfig}'s
     * {@code authenticated()} rule (e.g. a missing/invalid JWT caught by
     * {@code JwtAuthFilter}) to 401.
     */
    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ApiResponse<Object>> handleAuthentication(AuthenticationException ex) {
        log.debug("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Authentication required"));
    }
}
